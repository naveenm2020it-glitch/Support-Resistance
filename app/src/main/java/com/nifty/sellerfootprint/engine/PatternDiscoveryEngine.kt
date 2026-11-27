package com.nifty.sellerfootprint.engine

import com.nifty.sellerfootprint.model.DiscoveredPatternRecord
import com.nifty.sellerfootprint.model.OptionSide
import com.nifty.sellerfootprint.model.PatternId
import com.nifty.sellerfootprint.model.PatternValidationStatus
import com.nifty.sellerfootprint.model.SessionMarketData
import com.nifty.sellerfootprint.model.StrikeBelt
import com.nifty.sellerfootprint.model.StrikeMicrostructureMetrics
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object PatternDiscoveryEngine {

    data class StrikePatternMatch(
        val strikePrice: Double,
        val optionType: OptionSide,
        val matchedPatterns: List<PatternId>,
        val hasVetoSignal: Boolean,
        val isFilteredTailWing: Boolean
    )

    data class TwentyTwoSessionDiscoveryOutcome(
        val phase1LedgerA: List<DiscoveredPatternRecord>,
        val phase2ValidationNotes: List<String>,
        val phase2NovelLedgerC: List<DiscoveredPatternRecord>,
        val combinedLedgerD: List<DiscoveredPatternRecord>,
        val session22Metrics: List<StrikeMicrostructureMetrics>,
        val session22Envelope: MultiStrikeAnalyzer.ChainEnvelopeResult,
        val session22StrikeMatches: List<StrikePatternMatch>,
        val session22MatchedCodes: List<String>,
        val session22UnvalidatedObservations: List<String>
    )

    /**
     * Executes the complete 4-phase 22-session research pipeline:
     * - Phase A (Sessions 1..17): Independent pattern discovery -> Ledger A (frozen at session 17)
     * - Phase B (Sessions 18..21):
     *     Track 2A: Out-of-sample validation of frozen Ledger A -> Ledger B
     *     Track 2B: Independent discovery of novel regime patterns -> Ledger C
     * - Phase C: Transparent synthesis & rejection of unreliable patterns -> Ledger D
     * - Phase D (Session 22): Blind EOD pattern test on [targetExpiry] + unmatched anomaly logging
     */
    fun runTwentyTwoSessionPipeline(
        sessions: List<SessionMarketData>,
        targetExpiry: LocalDate
    ): TwentyTwoSessionDiscoveryOutcome {
        require(sessions.size >= 22) { "Expected 22 sessions, got ${sessions.size}" }

        val phase1Sessions = sessions.subList(0, 17)  // index 0..16 (Sessions 1..17)
        val phase2Sessions = sessions.subList(17, 21) // index 17..20 (Sessions 18..21)
        val session22 = sessions[21]                  // index 21 (Session 22)

        // Precompute active-chain metrics for each session using its nearest active expiry
        val sessionMetricsMap = sessions.mapIndexed { idx, s ->
            val prior = sessions.subList(0, idx)
            val activeExp = if (idx == 21) {
                targetExpiry
            } else {
                s.contracts.map { it.expiryDate }
                    .filter { !it.isBefore(s.tradeDate) }
                    .minOrNull() ?: targetExpiry
            }
            MultiStrikeAnalyzer.analyzeSessionChain(s, activeExp, prior)
        }

        // --- PHASE A: Sessions 1..17 Discovery (Ledger A) ---
        val phase1Stats = evaluatePatternsOverWindow(
            windowIndices = 0..16,
            sessions = sessions,
            metricsPerSession = sessionMetricsMap,
            candidatePatterns = listOf(
                PatternId.P1_ABSORPTION_WRITING,
                PatternId.P2_MULTI_SESSION_ANCHOR,
                PatternId.P3_INTRADAY_REJECTION_OI,
                PatternId.P4_STRANGLE_SKEW_PIN,
                PatternId.P5_VERTICAL_DEFENSIVE_ROLL,
                PatternId.P6_SHORT_COVERING_CAPITULATION,
                PatternId.P7_TAIL_WING_MARGIN_HEDGE
            )
        )

        val ledgerA = phase1Stats.map { (patternId, stat) ->
            val status = when (patternId) {
                PatternId.P7_TAIL_WING_MARGIN_HEDGE -> PatternValidationStatus.FILTER_RULE
                PatternId.P6_SHORT_COVERING_CAPITULATION -> PatternValidationStatus.VETO_RULE
                else -> PatternValidationStatus.VALIDATED_TIER_1
            }
            DiscoveredPatternRecord(
                patternId = patternId,
                occurrencesInDiscovery = stat.triggers,
                inSampleHoldRate = stat.holdRate,
                outOfSampleOccurrences = 0,
                outOfSampleHoldRate = null,
                status = status,
                evidenceSummary = "Sessions 1–17: triggered ${stat.triggers} time(s); " +
                    "next-session zone hold rate ${(stat.holdRate * 100).toInt()}%."
            )
        }.filter { it.occurrencesInDiscovery > 0 }

        // --- PHASE B (Track 2A): Out-of-Sample Validation on Sessions 18..21 ---
        val phase2ValidationStats = evaluatePatternsOverWindow(
            windowIndices = 17..20,
            sessions = sessions,
            metricsPerSession = sessionMetricsMap,
            candidatePatterns = ledgerA.map { it.patternId }
        )

        val validationNotes = mutableListOf<String>()
        val reconciledFromPhase1 = ledgerA.map { record ->
            val oos = phase2ValidationStats[record.patternId]
            val oosCount = oos?.triggers ?: 0
            val oosHold = if (oosCount > 0) oos?.holdRate else null

            val finalStatus = when {
                record.patternId == PatternId.P7_TAIL_WING_MARGIN_HEDGE -> PatternValidationStatus.FILTER_RULE
                record.patternId == PatternId.P6_SHORT_COVERING_CAPITULATION -> PatternValidationStatus.VETO_RULE
                oosCount >= 2 && oosHold != null && oosHold < 0.45 -> PatternValidationStatus.REJECTED_OVERFITTED
                else -> PatternValidationStatus.VALIDATED_TIER_1
            }

            val note = when {
                record.patternId == PatternId.P7_TAIL_WING_MARGIN_HEDGE ->
                    "P7 (Tail-Wing Margin Hedge): Confirmed noise filter (${oosCount} far-OTM wing hits excluded in Sessions 18–21)."
                record.patternId == PatternId.P6_SHORT_COVERING_CAPITULATION ->
                    "P6 (Short-Covering Capitulation): Active contradiction veto ($oosCount unwind breaches detected in Sessions 18–21)."
                finalStatus == PatternValidationStatus.REJECTED_OVERFITTED ->
                    "${record.patternId.code} (${record.patternId.title}): REJECTED — out-of-sample hold rate dropped to ${(oosHold!! * 100).toInt()}% across $oosCount tests (overfitted to Phase 1)."
                oosCount > 0 ->
                    "${record.patternId.code} (${record.patternId.title}): VALIDATED — held ${(oosHold!! * 100).toInt()}% across $oosCount out-of-sample tests in Sessions 18–21."
                else ->
                    "${record.patternId.code} (${record.patternId.title}): RETAINED (Conditional) — 0 triggers in Sessions 18–21; preserved from Phase 1 (${record.occurrencesInDiscovery} triggers, ${(record.inSampleHoldRate * 100).toInt()}% hold)."
            }
            validationNotes.add(note)

            record.copy(
                outOfSampleOccurrences = oosCount,
                outOfSampleHoldRate = oosHold,
                status = finalStatus
            )
        }

        // --- PHASE B (Track 2B): Independent Novel Discovery on Sessions 18..21 (Ledger C) ---
        val novelCandidates = listOf(
            PatternId.NP1_CALENDAR_ROLLOVER_WALL,
            PatternId.NP2_VOL_EXPANSION_HARVEST,
            PatternId.NP3_GAMMA_PIN_MAGNET
        )
        val novelStats = evaluatePatternsOverWindow(
            windowIndices = 17..20,
            sessions = sessions,
            metricsPerSession = sessionMetricsMap,
            candidatePatterns = novelCandidates
        )

        val ledgerC = novelCandidates.mapNotNull { pid ->
            val st = novelStats[pid] ?: return@mapNotNull null
            if (st.triggers <= 0) return@mapNotNull null
            DiscoveredPatternRecord(
                patternId = pid,
                occurrencesInDiscovery = st.triggers,
                inSampleHoldRate = st.holdRate,
                outOfSampleOccurrences = st.triggers,
                outOfSampleHoldRate = st.holdRate,
                status = PatternValidationStatus.NOVEL_PHASE_B,
                evidenceSummary = "Discovered independently in Sessions 18–21 (${st.triggers} occurrence(s), " +
                    "subsequent-session hold rate ${(st.holdRate * 100).toInt()}%). Tagged as regime-specific."
            )
        }

        // --- PHASE C: Combined Pattern Set (Ledger D) ---
        val combinedLedgerD = (
            reconciledFromPhase1.filter { it.status != PatternValidationStatus.REJECTED_OVERFITTED } +
                ledgerC
            )
        val activePatternIds = combinedLedgerD.map { it.patternId }.toSet()

        // --- PHASE D: Session 22 Final EOD Test ---
        val s22Envelope = sessionMetricsMap[21]
        val s22Metrics = s22Envelope.metrics
        val s22MatchedCodes = linkedSetOf<String>()
        val s22UnvalidatedObservations = mutableListOf<String>()

        val strikeMatches = s22Metrics.map { m ->
            val matched = detectPatternsForStrike(
                metric = m,
                session = session22,
                allSessionMetrics = s22Metrics,
                prevSession = sessions[20]
            ).filter { it in activePatternIds }

            matched.forEach { s22MatchedCodes.add("${it.code}: ${it.title}") }

            val hasVeto = PatternId.P6_SHORT_COVERING_CAPITULATION in matched
            val isTailWing = PatternId.P7_TAIL_WING_MARGIN_HEDGE in matched

            // Log genuine Session 22 anomalies that did not match any Ledger D pattern
            if (matched.isEmpty() && abs(m.zDeltaOi) >= 2.2 && m.belt != StrikeBelt.TAIL_HEDGE) {
                s22UnvalidatedObservations.add(
                    "Unmatched Session 22 anomaly at ${m.strikePrice.toInt()} ${m.optionType}: " +
                        "Z(ΔOI)=${String.format("%.2f", m.zDeltaOi)}, ΔOI=${m.deltaOiLots.toLong()} lots, " +
                        "PRR=${String.format("%.2f", m.positionalRetentionRatio)} (logged separately; not used as validated pattern)."
                )
            }

            StrikePatternMatch(
                strikePrice = m.strikePrice,
                optionType = m.optionType,
                matchedPatterns = matched,
                hasVetoSignal = hasVeto,
                isFilteredTailWing = isTailWing
            )
        }

        return TwentyTwoSessionDiscoveryOutcome(
            phase1LedgerA = ledgerA,
            phase2ValidationNotes = validationNotes,
            phase2NovelLedgerC = ledgerC,
            combinedLedgerD = combinedLedgerD,
            session22Metrics = s22Metrics,
            session22Envelope = s22Envelope,
            session22StrikeMatches = strikeMatches,
            session22MatchedCodes = s22MatchedCodes.toList(),
            session22UnvalidatedObservations = s22UnvalidatedObservations.take(5)
        )
    }

    private data class PatternEvaluationStat(
        val triggers: Int,
        val holds: Int
    ) {
        val holdRate: Double
            get() = if (triggers > 0) holds.toDouble() / triggers.toDouble() else 0.0
    }

    private fun evaluatePatternsOverWindow(
        windowIndices: IntRange,
        sessions: List<SessionMarketData>,
        metricsPerSession: List<MultiStrikeAnalyzer.ChainEnvelopeResult>,
        candidatePatterns: List<PatternId>
    ): Map<PatternId, PatternEvaluationStat> {
        val triggersMap = mutableMapOf<PatternId, Int>()
        val holdsMap = mutableMapOf<PatternId, Int>()

        for (idx in windowIndices) {
            val currentSession = sessions[idx]
            val prevSession = if (idx > 0) sessions[idx - 1] else null
            val nextSession = if (idx + 1 < sessions.size) sessions[idx + 1] else null
            val currentMetrics = metricsPerSession[idx].metrics

            for (m in currentMetrics) {
                // Evaluate relevant OTM/ATM seller strikes (CE >= spot - 100, PE <= spot + 100)
                val spot = currentSession.underlyingClose
                val relevantSide = when (m.optionType) {
                    OptionSide.CE -> m.strikePrice >= spot - 100.0
                    OptionSide.PE -> m.strikePrice <= spot + 100.0
                }
                if (!relevantSide) continue

                val detected = detectPatternsForStrike(m, currentSession, currentMetrics, prevSession)
                for (pid in detected) {
                    if (pid !in candidatePatterns) continue
                    triggersMap[pid] = (triggersMap[pid] ?: 0) + 1

                    if (nextSession != null) {
                        val held = when (m.optionType) {
                            OptionSide.CE -> nextSession.underlyingClose <= m.strikePrice + m.settlePrice
                            OptionSide.PE -> nextSession.underlyingClose >= m.strikePrice - m.settlePrice
                        }
                        if (held) {
                            holdsMap[pid] = (holdsMap[pid] ?: 0) + 1
                        }
                    }
                }
            }
        }

        return candidatePatterns.associateWith { pid ->
            PatternEvaluationStat(
                triggers = triggersMap[pid] ?: 0,
                holds = holdsMap[pid] ?: 0
            )
        }
    }

    fun detectPatternsForStrike(
        metric: StrikeMicrostructureMetrics,
        session: SessionMarketData,
        allSessionMetrics: List<StrikeMicrostructureMetrics>,
        prevSession: SessionMarketData?
    ): List<PatternId> {
        val found = mutableListOf<PatternId>()
        val spot = session.underlyingClose
        val spotDelta = if (prevSession != null) spot - prevSession.underlyingClose else 0.0

        // P7: Tail-Wing Margin Hedge (Noise Filter)
        if (metric.belt == StrikeBelt.TAIL_HEDGE && metric.settlePrice < 18.0 && metric.oiLots > 0.0) {
            found.add(PatternId.P7_TAIL_WING_MARGIN_HEDGE)
            return found
        }

        // P6: Short-Covering Capitulation (Veto Signal)
        val spotBreachedStrike = when (metric.optionType) {
            OptionSide.CE -> spot > metric.strikePrice
            OptionSide.PE -> spot < metric.strikePrice
        }
        if (spotBreachedStrike && metric.zDeltaOi <= -1.35 && metric.priceChange > 0.0) {
            found.add(PatternId.P6_SHORT_COVERING_CAPITULATION)
        }

        // P1: Absorption Writing (Price-OI Divergence)
        val movedTowardStrike = when (metric.optionType) {
            OptionSide.CE -> spotDelta > 0.0 && metric.strikePrice >= spot - 50.0
            OptionSide.PE -> spotDelta < 0.0 && metric.strikePrice <= spot + 50.0
        }
        if (metric.zDeltaOi >= 1.15 &&
            metric.positionalRetentionRatio >= 0.14 &&
            (movedTowardStrike && metric.excessPriceDecay < -1.0 || metric.priceChange <= 0.0)
        ) {
            found.add(PatternId.P1_ABSORPTION_WRITING)
        }

        // P2: Multi-Session Anchor Wall
        if (metric.persistenceSessions >= 2 && metric.deltaOiLots >= -500.0 && metric.oiLots > 0.0) {
            found.add(PatternId.P2_MULTI_SESSION_ANCHOR)
        }

        // P3: Intraday Rejection Wick + OI Addition
        val intradayProbedAndRejected = when (metric.optionType) {
            OptionSide.CE -> session.underlyingHigh >= metric.strikePrice - (spot * 0.0025) &&
                spot < metric.strikePrice
            OptionSide.PE -> session.underlyingLow <= metric.strikePrice + (spot * 0.0025) &&
                spot > metric.strikePrice
        }
        if (intradayProbedAndRejected && metric.zDeltaOi >= 0.90 && metric.atsRatioToMedian >= 1.15) {
            found.add(PatternId.P3_INTRADAY_REJECTION_OI)
        }

        // P4: Asymmetric Strangle Skew Pin
        val hasOppositeStrongWriter = allSessionMetrics.any { other ->
            other.optionType != metric.optionType &&
                other.belt != StrikeBelt.TAIL_HEDGE &&
                other.zDeltaOi >= 1.15
        }
        if (metric.zDeltaOi >= 1.15 && metric.belt == StrikeBelt.PRIMARY_WALL && hasOppositeStrongWriter) {
            found.add(PatternId.P4_STRANGLE_SKEW_PIN)
        }

        // P5: Vertical Defensive Roll
        if (metric.isPairedWithOppositeRoll && metric.zDeltaOi >= 1.15) {
            found.add(PatternId.P5_VERTICAL_DEFENSIVE_ROLL)
        }

        // NP1: Monthly-to-Weekly Calendar Rollover Wall (DTE <= 2 with heavy institutional block addition)
        val dte = ChronoUnit.DAYS.between(session.tradeDate, metric.expiryDate)
        if (dte in 0..2 && metric.zDeltaOi >= 1.30 && metric.atsRatioToMedian >= 1.35) {
            found.add(PatternId.NP1_CALENDAR_ROLLOVER_WALL)
        }

        // NP2: Volatility-Expansion Premium Harvest
        val curVol = session.volatilityRecord?.dailyVolatility
        val prevVol = prevSession?.volatilityRecord?.dailyVolatility
        if (curVol != null && prevVol != null && prevVol > 0.0 &&
            (curVol - prevVol) / prevVol >= 0.10 &&
            metric.belt == StrikeBelt.PRIMARY_WALL &&
            metric.zDeltaOi >= 1.05
        ) {
            found.add(PatternId.NP2_VOL_EXPANSION_HARVEST)
        }

        // NP3: Near-Expiry ATM Gamma-Pin Magnet
        if (dte in 0..1 && abs(metric.distanceFromSpotPct) <= 0.35) {
            val pairedOppositeAtSameStrike = allSessionMetrics.firstOrNull {
                it.strikePrice == metric.strikePrice && it.optionType != metric.optionType
            }
            if (pairedOppositeAtSameStrike != null &&
                metric.oiLots > 0.0 &&
                pairedOppositeAtSameStrike.oiLots > 0.0 &&
                metric.priceChange <= 0.0 &&
                pairedOppositeAtSameStrike.priceChange <= 0.0
            ) {
                found.add(PatternId.NP3_GAMMA_PIN_MAGNET)
            }
        }

        return found
    }
}
