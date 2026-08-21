package com.lagradost.cloudstream3.utils

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Ad blocker interceptor that blocks common ad domains.
 * Does NOT block video streams or content.
 */
object AdBlocker {

    private val blockedDomains = setOf(
        // Common ad networks
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adnxs.com",
        "adsrvr.org",
        "advertising.com",
        "amazon-adsystem.com",
        "moatads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "comscore.com",
        "chartbeat.com",
        "hotjar.com",
        "crazyegg.com",
        "optimizely.com",
        "segment.io",
        "segment.com",
        "mixpanel.com",
        "amplitude.com",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "kochava.com",
        "singular.net",
        "tenjin.com",
        "devapp.com",
        "unity3d.com",
        "unityads.unity3d.com",
        "adcolony.com",
        "vungle.com",
        "ironsrc.com",
        "applovin.com",
        "startapp.com",
        "inmobi.com",
        "tapjoy.com",
        "fyber.com",
        "smaato.net",
        "mopub.com",
        "flurry.com",
        "chartboost.com",
        "revenuecat.com",
        "onesignal.com",
        "pushwoosh.com",
        "clevertap.com",
        "airship.com",
        "batch.com",
        "braze.com",
        "iterable.com",
        "leanplum.com",
        "urbanairship.com",
        "localytics.com",
        "kissmetrics.com",
        "heap.io",
        "pendo.io",
        "fullstory.com",
        "logrocket.com",
        "datadoghq.com",
        "bugsnag.com",
        "sentry.io",
        "crashlytics.com",
        "firebase.com",
        "firebaseio.com",
        "firebasestorage.googleapis.com",
        "app-measurement.com",
        "android.clients.google.com",
        // Crypto miners
        "coinhive.com",
        "coinlab.biz",
        "jsecoin.com",
        "authedmine.com",
        "ppoi.org",
        "cryptoloot.pro",
        "crypto-loot.com",
        "webminepool.com",
        "minero.cc",
        "minr.pw",
        // Tracking
        "facebook.com/tr",
        "facebook.net",
        "connect.facebook.net",
        "twitter.com/i/adsct",
        "analytics.twitter.com",
        "t.co",
        "linkedin.com/px",
        "snap.licdn.com",
        "pinimg.com/ct",
        "reddit.com/track",
        "tiktok.com/pixel",
        "snapchat.com/tr",
        // Pop-ups and malvertising
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "monetizebot.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.com",
        "erosvet.com",
        "adnxs.com",
        "serving-sys.com",
        "adform.net",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "turn.com",
        "bidswitch.net",
        "sharethrough.com",
        "outbrain.com",
        "taboola.com",
        "revcontent.com",
        "mgid.com",
        "contentad.net",
        "spotxchange.com",
        "teads.tv",
        "prebid.org"
    )

    private val blockedPatterns = listOf(
        Regex(".*\\.ads\\..*"),
        Regex(".*\\.ad\\..*"),
        Regex(".*\\.analytics\\..*"),
        Regex(".*\\.tracking\\..*"),
        Regex(".*\\.tracker\\..*"),
        Regex(".*\\.telemetry\\..*"),
        Regex(".*advertising.*"),
        Regex(".*doubleclick.*"),
        Regex(".*googlesyndication.*"),
        Regex(".*googleadservices.*")
    )

    fun createInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            val host = request.url.host

            // Check if request should be blocked
            if (shouldBlock(host, url)) {
                // Return empty response for blocked requests
                return@Interceptor Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("Blocked")
                    .body("".toResponseBody(null))
                    .build()
            }

            chain.proceed(request)
        }
    }

    private fun shouldBlock(host: String, url: String): Boolean {
        // Check exact domain matches
        if (blockedDomains.any { host.endsWith(it) || host == it }) {
            return true
        }

        // Check pattern matches
        if (blockedPatterns.any { it.matches(url) }) {
            return true
        }

        return false
    }
}
