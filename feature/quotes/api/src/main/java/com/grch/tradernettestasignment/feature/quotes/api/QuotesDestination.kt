package com.grch.tradernettestasignment.feature.quotes.api

/**
 * Public identifier for the quotes feature destination.
 *
 * The app entry point presents this destination. The foundation does not
 * introduce a navigation framework; the route is the stable integration
 * identifier later navigation wiring can build on.
 */
object QuotesDestination {
    const val ROUTE: String = "quotes"
}
