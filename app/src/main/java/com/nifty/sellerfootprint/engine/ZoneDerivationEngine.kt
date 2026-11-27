package com.nifty.sellerfootprint.engine

import com.nifty.sellerfootprint.model.FootprintZone
import com.nifty.sellerfootprint.model.OptionSide
import com.nifty.sellerfootprint.model.PatternId
import com.nifty.sellerfootprint.model.SessionMarketData
import com.nifty.sellerfootprint.model.StrikeMicrostructureMetrics
import com.nifty.sellerfootprint.model.ZoneEvidenceStrength
import com.nifty.sellerfootprint.model.ZoneRole
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object ZoneDerivationEngine {

    data class DerivedZonesBundle(
        val s1: FootprintZone,
        val s2: FootprintZone,
        val r1: FootprintZone,
        val r2: FootprintZone,
        val strongestZoneSummary: String,
        val weakestZoneSummary: String,
        val persistentAreasSummary: String,
        val newlyDetectedSummary: String,
        val shiftingSummary: String
    )

    private data class CandidateCluster(
        val side: OptionSide,
        val strikes: List<StrikeMicrostructureMetrics>,
        val matchedPatternIds: Set<PatternId>,
        val lowerBound: Double,
        val upperBound: Double,
        val midpoint: Double,
        val evidencePoints: Int,
        val strength: ZoneEvidenceStrength,
        val totalOiLots: Long,
        val deltaOiLots: Long,
        val avgSettlePrice: Double,
        val avgPrr: Double,
        val maxAtsRatio: Double,
        val maxPersistence: Int,
        val participantAlignmentNote: String,
        val rollSourceStrike: Double?
    )

    fun deriveNarrowZones(
        session22: SessionMarketData,
        prevSession21: SessionMarketData,
        targetExpiry: LocalDate,
        discovery: PatternDiscoveryEngine.TwentyTwoSessionDiscoveryOutcome
    ): DerivedZonesBundle {
        val spot = session22.underlyingClose
        val dte = max(0L, ChronoUnit.DAYS.between(session22.tradeDate, targetExpiry))
        val cushionWeight = if (dte <= 2L) 1.0 else 0.5

        val matchByKey = discovery.session22StrikeMatches.associateBy {
            Pair(it.optionType, it.strikePrice)
        }

        // Participant-Wise OI alignment check (Pro + FII net change on Session 22)
        val proCallShortDelta = netShortDelta(session22, prevSession21, "Pro", isCall = true)
        val fiiCallShortDelta = netShortDelta(session22, prevSession21, "FII", isCall = true)
        val proPutShortDelta = netShortDelta(session22, prevSession21, "Pro", isCall = false)
        val fiiPutShortDelta = netShortDelta(session22, prevSession21, "FII", isCall = false)

        val validSupportMetrics = discovery.session22Metrics.filter { m ->
            val match = matchByKey[Pair(m.optionType, m.strikePrice)]
            m.optionType == OptionSide.PE &&
                m.strikePrice <= spot &&
                m.strikePrice >= spot * 0.90 &&
                match != null &&
                !match.isFilteredTailWing &&
                !match.hasVetoSignal &&
                (match.matchedPatterns.isNotEmpty() || m.zDeltaOi >= 1.25 || m.persistenceSessions >= 2)
        }.sortedByDescending { it.strikePrice } // closest below spot first

        val validResistanceMetrics = discovery.session22Metrics.filter { m ->
            val match = matchByKey[Pair(m.optionType, m.strikePrice)]
            m.optionType == OptionSide.CE &&
                m.strikePrice >= spot &&
                m.strikePrice <= spot * 1.10 &&
                match != null &&
                !match.isFilteredTailWing &&
                !match.hasVetoSignal &&
                (match.matchedPatterns.isNotEmpty() || m.zDeltaOi >= 1.25 || m.persistenceSessions >= 2)
        }.sortedBy { it.strikePrice } // closest above spot first

        val supportClusters = buildClusters(
            orderedMetrics = validSupportMetrics,
            side = OptionSide.PE,
            cushionWeight = cushionWeight,
            matchByKey = matchByKey,
            proNetShortDelta = proPutShortDelta,
            fiiNetShortDelta = fiiPutShortDelta
        )

        val resistanceClusters = buildClusters(
            orderedMetrics = validResistanceMetrics,
            side = OptionSide.CE,
            cushionWeight = cushionWeight,
            matchByKey = matchByKey,
            proNetShortDelta = proCallShortDelta,
            fiiNetShortDelta = fiiCallShortDelta
        )

        // Select S1 (closest credible support below spot) & S2 (next distinct support below S1)
        val s1Cluster = supportClusters.firstOrNull { it.midpoint < spot }
        val s2Cluster = if (s1Cluster != null) {
            supportClusters.firstOrNull { c ->
                c.upperBound < s1Cluster.lowerBound &&
                    c.strikes.maxOf { it.strikePrice } < s1Cluster.strikes.minOf { it.strikePrice }
            }
        } else null

        // Select R1 (closest credible resistance above spot) & R2 (next distinct resistance above R1)
        val r1Cluster = resistanceClusters.firstOrNull { it.midpoint > spot }
        val r2Cluster = if (r1Cluster != null) {
            resistanceClusters.firstOrNull { c ->
                c.lowerBound > r1Cluster.upperBound &&
                    c.strikes.minOf { it.strikePrice } > r1Cluster.strikes.maxOf { it.strikePrice }
            }
        } else null

        val s1 = toFootprintZone(ZoneRole.S1, s1Cluster, spot, dte)
        val s2 = toFootprintZone(ZoneRole.S2, s2Cluster, spot, dte)
        val r1 = toFootprintZone(ZoneRole.R1, r1Cluster, spot, dte)
        val r2 = toFootprintZone(ZoneRole.R2, r2Cluster, spot, dte)

        val established = listOfNotNull(
            s1Cluster?.let { ZoneRole.S1 to it },
            s2Cluster?.let { ZoneRole.S2 to it },
            r1Cluster?.let { ZoneRole.R1 to it },
            r2Cluster?.let { ZoneRole.R2 to it }
        )

        val strongest = established.maxByOrNull { it.second.evidencePoints }
        val weakest = established.minByOrNull { it.second.evidencePoints }

        val strongestSummary = strongest?.let { (role, c) ->
            "${role.code} (${formatRange(c.lowerBound, c.upperBound)}) — ${c.strength.label} with ${c.matchedPatternIds.size} active pattern(s) & ${c.totalOiLots} lots OI"
        } ?: "None — insufficient multi-session seller concentration on Session 22"

        val weakestSummary = weakest?.let { (role, c) ->
            "${role.code} (${formatRange(c.lowerBound, c.upperBound)}) — ${c.strength.label}"
        } ?: "None established"

        val persistentStrikes = discovery.session22Metrics
            .filter { it.persistenceSessions >= 2 }
            .sortedByDescending { it.persistenceSessions }
            .take(4)
            .map { "${it.strikePrice.toInt()} ${it.optionType} (${it.persistenceSessions} sessions)" }
        val persistentSummary = if (persistentStrikes.isNotEmpty()) {
            persistentStrikes.joinToString(", ")
        } else {
            "No strikes maintained uninterrupted top-quintile OI persistence across prior sessions."
        }

        val freshFootprints = discovery.session22Metrics
            .filter { it.zDeltaOi >= 1.35 && it.persistenceSessions == 0 }
            .sortedByDescending { it.zDeltaOi }
            .take(3)
            .map { "${it.strikePrice.toInt()} ${it.optionType} (+${it.deltaOiLots.toLong()} lots)" }
        val newlyDetectedSummary = if (freshFootprints.isNotEmpty()) {
            freshFootprints.joinToString(", ")
        } else {
            "No isolated fresh Session 22 outlier additions outside existing walls."
        }

        val rollClusters = established.mapNotNull { (role, c) ->
            c.rollSourceStrike?.let { src ->
                "${role.code}: sellers rolled from ${src.toInt()} ${c.side} into ${c.strikes.first().strikePrice.toInt()} ${c.side}"
            }
        }
        val shiftingSummary = if (rollClusters.isNotEmpty()) {
            rollClusters.joinToString("; ")
        } else {
            "No vertical strike-to-strike defensive roll detected on Session 22."
        }

        return DerivedZonesBundle(
            s1 = s1,
            s2 = s2,
            r1 = r1,
            r2 = r2,
            strongestZoneSummary = strongestSummary,
            weakestZoneSummary = weakestSummary,
            persistentAreasSummary = persistentSummary,
            newlyDetectedSummary = newlyDetectedSummary,
            shiftingSummary = shiftingSummary
        )
    }

    private fun buildClusters(
        orderedMetrics: List<StrikeMicrostructureMetrics>,
        side: OptionSide,
        cushionWeight: Double,
        matchByKey: Map<Pair<OptionSide, Double>, PatternDiscoveryEngine.StrikePatternMatch>,
        proNetShortDelta: Long?,
        fiiNetShortDelta: Long?
    ): List<CandidateCluster> {
        if (orderedMetrics.isEmpty()) return emptyList()

        // Group adjacent 50-point strikes into narrow clusters (max 2 strikes per narrow cluster)
        val rawGroups = mutableListOf<MutableList<StrikeMicrostructureMetrics>>()
        for (m in orderedMetrics) {
            val lastGroup = rawGroups.lastOrNull()
            if (lastGroup != null &&
                lastGroup.size < 2 &&
                abs(lastGroup.last().strikePrice - m.strikePrice) <= 50.0 &&
                m.zDeltaOi >= 0.85 &&
                lastGroup.last().zDeltaOi >= 0.85
            ) {
                lastGroup.add(m)
            } else {
                rawGroups.add(mutableListOf(m))
            }
        }

        return rawGroups.map { group ->
            val patterns = group.flatMap {
                matchByKey[Pair(it.optionType, it.strikePrice)]?.matchedPatterns.orEmpty()
            }.toSet()

            val totalOi = group.sumOf { max(0.0, it.oiLots) }.toLong()
            val deltaOi = group.sumOf { it.deltaOiLots }.toLong()
            val avgSettle = group.map { it.settlePrice }.average()
            val avgPrr = group.map { it.positionalRetentionRatio }.average()
            val maxAts = group.maxOf { it.atsRatioToMedian }
            val maxPersist = group.maxOf { it.persistenceSessions }
            val rollSrc = group.firstNotNullOfOrNull { it.oppositeRollSourceStrike }

            val cushion = roundToQuarter(cushionWeight * avgSettle).coerceIn(8.0, 55.0)
            val lower: Double
            val upper: Double
            if (side == OptionSide.PE) {
                val highestStrike = group.maxOf { it.strikePrice }
                val lowestStrike = group.minOf { it.strikePrice }
                upper = highestStrike
                lower = roundToQuarter(lowestStrike - cushion)
            } else {
                val lowestStrike = group.minOf { it.strikePrice }
                val highestStrike = group.maxOf { it.strikePrice }
                lower = lowestStrike
                upper = roundToQuarter(highestStrike + cushion)
            }

            // Evidence rubric based strictly on empirical structural factors
            var points = 0
            if (patterns.any { it.originPhase.startsWith("Phase A") }) points += 2
            if (patterns.size >= 2) points += 2
            if (maxPersist >= 2) points += 2
            if (avgPrr >= 0.16) points += 1
            if (maxAts >= 1.25) points += 1
            val participantConfirmed = (proNetShortDelta != null && proNetShortDelta > 0L) ||
                (fiiNetShortDelta != null && fiiNetShortDelta > 0L)
            if (participantConfirmed) points += 1

            val strength = when {
                points >= 6 -> ZoneEvidenceStrength.STRONG
                points >= 3 -> ZoneEvidenceStrength.MODERATE
                else -> ZoneEvidenceStrength.WEAK
            }

            val participantNote = buildParticipantNote(side, proNetShortDelta, fiiNetShortDelta)

            CandidateCluster(
                side = side,
                strikes = group,
                matchedPatternIds = patterns,
                lowerBound = lower,
                upperBound = upper,
                midpoint = (lower + upper) / 2.0,
                evidencePoints = points,
                strength = strength,
                totalOiLots = totalOi,
                deltaOiLots = deltaOi,
                avgSettlePrice = avgSettle,
                avgPrr = avgPrr,
                maxAtsRatio = maxAts,
                maxPersistence = maxPersist,
                participantAlignmentNote = participantNote,
                rollSourceStrike = rollSrc
            )
        }
    }

    private fun toFootprintZone(
        role: ZoneRole,
        cluster: CandidateCluster?,
        spot: Double,
        dte: Long
    ): FootprintZone {
        if (cluster == null) {
            val sideText = if (role == ZoneRole.S1 || role == ZoneRole.S2) "below" else "above"
            return FootprintZone(
                role = role,
                isEstablished = false,
                lowerBoundary = null,
                upperBoundary = null,
                widthPoints = null,
                strength = ZoneEvidenceStrength.INCONCLUSIVE,
                relevantStrikes = emptyList(),
                optionSide = if (role == ZoneRole.S1 || role == ZoneRole.S2) OptionSide.PE else OptionSide.CE,
                supportingPatterns = emptyList(),
                totalOiLots = 0L,
                deltaOiLots = 0L,
                settlePriceAvg = 0.0,
                positionalRetentionRatio = 0.0,
                atsRatio = 0.0,
                persistenceSessions = 0,
                participantAlignment = "Insufficient strike concentration $sideText NIFTY close ($spot).",
                validationSummary = "No validated pattern cluster satisfied minimum structural criteria for ${role.code}.",
                session22TestResult = "Inconclusive — zone not forced due to weak/absent seller evidence.",
                selectionReason = "No credible seller footprint cluster was detected $sideText $spot for ${role.code}."
            )
        }

        val width = roundToQuarter(cluster.upperBound - cluster.lowerBound)
        val strikesStr = cluster.strikes.joinToString(" + ") { "${it.strikePrice.toInt()} ${it.optionType}" }
        val patternNames = if (cluster.matchedPatternIds.isNotEmpty()) {
            cluster.matchedPatternIds.map { "${it.code}: ${it.title}" }
        } else {
            listOf("High Normalized OI Impulse (Z >= 1.25) / Multi-Session Concentration")
        }

        val directionLabel = if (cluster.side == OptionSide.PE) "support" else "resistance"
        val orderLabel = if (role == ZoneRole.S1 || role == ZoneRole.R1) "Closest" else "Next distinct"

        return FootprintZone(
            role = role,
            isEstablished = true,
            lowerBoundary = cluster.lowerBound,
            upperBoundary = cluster.upperBound,
            widthPoints = width,
            strength = cluster.strength,
            relevantStrikes = cluster.strikes.map { it.strikePrice },
            optionSide = cluster.side,
            supportingPatterns = patternNames,
            totalOiLots = cluster.totalOiLots,
            deltaOiLots = cluster.deltaOiLots,
            settlePriceAvg = cluster.avgSettlePrice,
            positionalRetentionRatio = cluster.avgPrr,
            atsRatio = cluster.maxAtsRatio,
            persistenceSessions = cluster.maxPersistence,
            participantAlignment = cluster.participantAlignmentNote,
            validationSummary = if (cluster.matchedPatternIds.any { it.originPhase.startsWith("Phase A") }) {
                "Backed by Phase-1 patterns validated out-of-sample in Sessions 18–21 (Ledger D)."
            } else {
                "Supported by Session 22 OI concentration & Ledger D regime signals."
            },
            session22TestResult = "Active on Session 22 without P6 short-covering capitulation veto (DTE = ${dte}d).",
            selectionReason = "$orderLabel credible $directionLabel zone relative to NIFTY close (${String.format("%.2f", spot)}), " +
                "anchored at $strikesStr with ${String.format("%.1f", width)}-pt premium-adjusted boundary."
        )
    }

    private fun netShortDelta(
        cur: SessionMarketData,
        prev: SessionMarketData,
        clientType: String,
        isCall: Boolean
    ): Long? {
        val cRec = cur.participantOi.firstOrNull { it.clientType.equals(clientType, ignoreCase = true) } ?: return null
        val pRec = prev.participantOi.firstOrNull { it.clientType.equals(clientType, ignoreCase = true) } ?: return null
        return if (isCall) {
            cRec.optionIndexCallShort - pRec.optionIndexCallShort
        } else {
            cRec.optionIndexPutShort - pRec.optionIndexPutShort
        }
    }

    private fun buildParticipantNote(
        side: OptionSide,
        proDelta: Long?,
        fiiDelta: Long?
    ): String {
        val leg = if (side == OptionSide.CE) "Index Call Short" else "Index Put Short"
        if (proDelta == null && fiiDelta == null) {
            return "Participant-Wise OI report unavailable for Session 22; evaluated via strike microstructure."
        }
        val proText = proDelta?.let { "Pro $leg: ${if (it >= 0) "+$it" else "$it"}" } ?: "Pro: N/A"
        val fiiText = fiiDelta?.let { "FII $leg: ${if (it >= 0) "+$it" else "$it"}" } ?: "FII: N/A"
        return "$proText | $fiiText contracts"
    }

    private fun roundToQuarter(value: Double): Double = round(value * 4.0) / 4.0

    private fun formatRange(low: Double, high: Double): String =
        "${String.format("%.2f", low)} – ${String.format("%.2f", high)}"
}
