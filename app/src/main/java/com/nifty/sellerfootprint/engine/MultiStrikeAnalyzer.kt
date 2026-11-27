package com.nifty.sellerfootprint.engine

import com.nifty.sellerfootprint.model.OptionContractRecord
import com.nifty.sellerfootprint.model.OptionSide
import com.nifty.sellerfootprint.model.SessionMarketData
import com.nifty.sellerfootprint.model.StrikeBelt
import com.nifty.sellerfootprint.model.StrikeMicrostructureMetrics
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MultiStrikeAnalyzer {

    data class ChainEnvelopeResult(
        val metrics: List<StrikeMicrostructureMetrics>,
        val envelopeMinStrike: Double,
        val envelopeMaxStrike: Double,
        val totalStrikesInExpiry: Int,
        val retainedStrikesCount: Int
    )

    /**
     * Dynamically discovers the relevant active strike range for [session] and [targetExpiry]
     * without restricting to ATM or a fixed strike count:
     * 1. Retains the contiguous strike span centered on spot capturing 95% of cumulative CE+PE OI mass.
     * 2. Bounds outer noise using a 2.5-sigma volatility envelope derived from FOVOLT (or realized returns).
     * 3. Computes per-strike microstructure features (Z-score delta OI, excess price decay, PRR, ATS ratio,
     *    multi-session persistence, and vertical roll linkage).
     */
    fun analyzeSessionChain(
        session: SessionMarketData,
        targetExpiry: LocalDate,
        priorSessions: List<SessionMarketData>
    ): ChainEnvelopeResult {
        val spot = session.underlyingClose
        val expiryContracts = session.contracts.filter { it.expiryDate == targetExpiry }
        if (spot <= 0.0 || expiryContracts.isEmpty()) {
            return ChainEnvelopeResult(emptyList(), 0.0, 0.0, 0, 0)
        }

        val allStrikesSorted = expiryContracts.map { it.strikePrice }.distinct().sorted()
        val totalOiAllStrikes = expiryContracts.sumOf { max(0.0, it.openInterestLots) }

        // 1. 95% Cumulative OI Mass envelope centered around spot
        val strikesByDistance = allStrikesSorted.sortedBy { abs(it - spot) }
        val oiByStrike = expiryContracts.groupBy { it.strikePrice }
            .mapValues { (_, list) -> list.sumOf { max(0.0, it.openInterestLots) } }

        var cumulativeOi = 0.0
        val massSelectedStrikes = mutableSetOf<Double>()
        for (k in strikesByDistance) {
            massSelectedStrikes.add(k)
            cumulativeOi += oiByStrike[k] ?: 0.0
            if (totalOiAllStrikes > 0.0 && cumulativeOi / totalOiAllStrikes >= 0.95) {
                break
            }
        }

        // Ensure at least immediate neighborhood if OI is heavily concentrated on 1-2 strikes
        val minMassStrike = massSelectedStrikes.minOrNull() ?: (spot * 0.965)
        val maxMassStrike = massSelectedStrikes.maxOrNull() ?: (spot * 1.035)

        // 2. 2.5-Sigma Volatility Horizon Envelope
        val dailyVol = resolveDailyVolatility(session, priorSessions)
        val dte = max(1L, ChronoUnit.DAYS.between(session.tradeDate, targetExpiry))
        val sigmaMove = spot * dailyVol * sqrt(dte.toDouble()) * 2.5
        val volLower = spot - max(sigmaMove, spot * 0.025)
        val volUpper = spot + max(sigmaMove, spot * 0.025)

        val envelopeMin = max(min(minMassStrike, volLower), allStrikesSorted.first())
        val envelopeMax = min(max(maxMassStrike, volUpper), allStrikesSorted.last())

        val retainedContracts = expiryContracts.filter {
            it.strikePrice in envelopeMin..envelopeMax
        }
        val retainedStrikesCount = retainedContracts.map { it.strikePrice }.distinct().size

        if (retainedContracts.isEmpty()) {
            return ChainEnvelopeResult(emptyList(), envelopeMin, envelopeMax, allStrikesSorted.size, 0)
        }

        // Cross-sectional statistics for Z-score & median ATS normalization
        val deltaOiValues = retainedContracts.map { it.changeInOiLots }
        val meanDeltaOi = deltaOiValues.average()
        val stdDeltaOi = standardDeviation(deltaOiValues, meanDeltaOi).let { if (it < 1.0) 1.0 else it }

        val atsValues = retainedContracts.mapNotNull { c ->
            if (c.totalTradesExecuted > 0 && c.tradedVolumeLots > 0.0) {
                c.tradedVolumeLots / c.totalTradesExecuted.toDouble()
            } else null
        }.sorted()
        val medianAts = if (atsValues.isNotEmpty()) atsValues[atsValues.size / 2] else 1.0

        val prevSession = priorSessions.lastOrNull()
        val spotDelta = if (prevSession != null && prevSession.underlyingClose > 0.0) {
            spot - prevSession.underlyingClose
        } else {
            session.indexSnapshot?.pointsChange ?: 0.0
        }

        // Identify strikes showing strong negative or positive Z_delta_OI for vertical roll pairing
        val zBySideAndStrike = retainedContracts.associate { c ->
            Pair(c.optionType, c.strikePrice) to ((c.changeInOiLots - meanDeltaOi) / stdDeltaOi)
        }

        val metricsList = retainedContracts.map { contract ->
            val distPct = ((contract.strikePrice - spot) / spot) * 100.0
            val absDistPct = abs(distPct)
            val belt = when {
                absDistPct <= 1.2 -> StrikeBelt.CORE_GAMMA
                absDistPct <= 3.5 -> StrikeBelt.PRIMARY_WALL
                else -> StrikeBelt.TAIL_HEDGE
            }

            val zDelta = (contract.changeInOiLots - meanDeltaOi) / stdDeltaOi
            val actualPriceChange = if (contract.prevClosePrice > 0.0) {
                contract.effectivePrice - contract.prevClosePrice
            } else {
                0.0
            }

            // Approximate directional delta sensitivity based on moneyness
            val moneynessDecay = exp(-0.22 * absDistPct)
            val approxDelta = when (contract.optionType) {
                OptionSide.CE -> if (contract.strikePrice <= spot) min(0.95, 0.5 + 0.45 * (1.0 - moneynessDecay)) else 0.5 * moneynessDecay
                OptionSide.PE -> if (contract.strikePrice >= spot) -min(0.95, 0.5 + 0.45 * (1.0 - moneynessDecay)) else -0.5 * moneynessDecay
            }
            val expectedPriceChange = spotDelta * approxDelta
            // Negative excessPriceDecay means option price underperformed theoretical delta (seller supply absorption)
            val excessDecay = actualPriceChange - expectedPriceChange

            val prr = if (contract.tradedVolumeLots > 0.0) {
                abs(contract.changeInOiLots) / contract.tradedVolumeLots
            } else {
                0.0
            }

            val ats = if (contract.totalTradesExecuted > 0) {
                contract.tradedVolumeLots / contract.totalTradesExecuted.toDouble()
            } else {
                0.0
            }
            val atsRatio = if (medianAts > 0.0) ats / medianAts else 1.0

            val persistence = computePersistenceSessions(
                strike = contract.strikePrice,
                side = contract.optionType,
                targetExpiry = targetExpiry,
                priorSessions = priorSessions
            )

            // Check if an inner strike (50..250 pts closer to spot) unwound OI while this strike added OI
            val rollSource = detectVerticalRollSource(
                strike = contract.strikePrice,
                side = contract.optionType,
                zDelta = zDelta,
                spot = spot,
                zMap = zBySideAndStrike
            )

            StrikeMicrostructureMetrics(
                tradeDate = session.tradeDate,
                expiryDate = targetExpiry,
                strikePrice = contract.strikePrice,
                optionType = contract.optionType,
                distanceFromSpotPct = distPct,
                belt = belt,
                oiLots = contract.openInterestLots,
                deltaOiLots = contract.changeInOiLots,
                zDeltaOi = zDelta,
                settlePrice = contract.effectivePrice,
                priceChange = actualPriceChange,
                excessPriceDecay = excessDecay,
                positionalRetentionRatio = prr,
                averageTradeSizeLots = ats,
                atsRatioToMedian = atsRatio,
                persistenceSessions = persistence,
                isPairedWithOppositeRoll = rollSource != null,
                oppositeRollSourceStrike = rollSource
            )
        }.sortedWith(compareBy({ it.strikePrice }, { it.optionType }))

        return ChainEnvelopeResult(
            metrics = metricsList,
            envelopeMinStrike = envelopeMin,
            envelopeMaxStrike = envelopeMax,
            totalStrikesInExpiry = allStrikesSorted.size,
            retainedStrikesCount = retainedStrikesCount
        )
    }

    private fun resolveDailyVolatility(
        current: SessionMarketData,
        priorSessions: List<SessionMarketData>
    ): Double {
        current.volatilityRecord?.dailyVolatility?.takeIf { it in 0.002..0.10 }?.let { return it }

        val closes = (priorSessions.takeLast(15) + current)
            .map { it.underlyingClose }
            .filter { it > 0.0 }
        if (closes.size >= 4) {
            val logReturns = (1 until closes.size).map { i -> ln(closes[i] / closes[i - 1]) }
            val mean = logReturns.average()
            val sd = standardDeviation(logReturns, mean)
            if (sd in 0.002..0.10) return sd
        }
        return 0.0095 // conservative 0.95% daily NIFTY baseline if volatility history is sparse
    }

    private fun computePersistenceSessions(
        strike: Double,
        side: OptionSide,
        targetExpiry: LocalDate,
        priorSessions: List<SessionMarketData>
    ): Int {
        var streak = 0
        for (past in priorSessions.asReversed()) {
            val sameExpiry = past.contracts.filter { it.expiryDate == targetExpiry && it.optionType == side }
            val fallbackAll = if (sameExpiry.isNotEmpty()) sameExpiry else past.contracts.filter { it.optionType == side }
            if (fallbackAll.isEmpty()) break

            val sortedOi = fallbackAll.map { it.openInterestLots }.sortedDescending()
            val topThresholdIdx = max(0, (sortedOi.size * 0.20).toInt() - 1)
            val topThresholdOi = sortedOi[topThresholdIdx]

            val match = fallbackAll.firstOrNull { it.strikePrice == strike } ?: break
            val breached = when (side) {
                OptionSide.CE -> past.underlyingClose > match.strikePrice + match.effectivePrice
                OptionSide.PE -> past.underlyingClose < match.strikePrice - match.effectivePrice
            }
            if (!breached && match.openInterestLots >= topThresholdOi && match.openInterestLots > 0.0) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun detectVerticalRollSource(
        strike: Double,
        side: OptionSide,
        zDelta: Double,
        spot: Double,
        zMap: Map<Pair<OptionSide, Double>, Double>
    ): Double? {
        if (zDelta < 1.15) return null
        val stepCandidates = listOf(50.0, 100.0, 150.0, 200.0, 250.0)
        for (step in stepCandidates) {
            val innerStrike = when (side) {
                OptionSide.CE -> strike - step
                OptionSide.PE -> strike + step
            }
            val innerValidSide = when (side) {
                OptionSide.CE -> innerStrike >= spot - 100.0
                OptionSide.PE -> innerStrike <= spot + 100.0
            }
            if (!innerValidSide) continue
            val innerZ = zMap[Pair(side, innerStrike)] ?: continue
            if (innerZ <= -1.10) {
                return innerStrike
            }
        }
        return null
    }

    private fun standardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(max(0.0, variance))
    }
}
