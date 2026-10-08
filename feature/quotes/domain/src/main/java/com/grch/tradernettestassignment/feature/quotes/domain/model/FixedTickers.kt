package com.grch.tradernettestassignment.feature.quotes.domain.model

/**
 * The fixed, ordered watchlist rendered by the quotes board.
 *
 * The order is required and MUST be preserved: updates never reorder, add, or
 * remove entries. Both the socket subscription
 * and the UI seed use this single source of truth.
 */
object FixedTickers {
    val ALL: List<String> = listOf(
        "SP500.IDX",
        "AAPL.US",
        "RSTI",
        "GAZP",
        "MRKZ",
        "RUAL",
        "HYDR",
        "MRKS",
        "SBER",
        "FEES",
        "TGKA",
        "VTBR",
        "ANH.US",
        "VICL.US",
        "BURG.US",
        "NBL.US",
        "YETI.US",
        "WSFS.US",
        "NIO.US",
        "DXC.US",
        "MIC.US",
        "HSBC.US",
        "EXPN.EU",
        "GSK.EU",
        "SHP.EU",
        "MAN.EU",
        "DB1.EU",
        "MUV2.EU",
        "TATE.EU",
        "KGF.EU",
        "MGGT.EU",
        "SGGD.EU",
        "AI.US",
    )
}
