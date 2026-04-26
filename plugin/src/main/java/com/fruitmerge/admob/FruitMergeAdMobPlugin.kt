package com.fruitmerge.admob

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

/**
 * FruitMergeAdMob — Consolidated Plugin (Single Singleton)
 *
 * Combines all ad types into one GodotPlugin class registered as "FruitMergeAdMob".
 * Methods are prefixed by ad type:
 *   - initialize(), set_request_configuration(), get_initialization_status()  (init — no prefix)
 *   - banner_create(), banner_load_ad(), banner_destroy(), ...                (banner)
 *   - rewarded_create(), rewarded_load(), rewarded_show(), ...                (rewarded)
 *   - interstitial_create(), interstitial_load(), interstitial_show(), ...    (interstitial)
 *   - rewarded_interstitial_create(), rewarded_interstitial_load(), ...       (rewarded interstitial)
 *   - native_create(), native_load(), native_destroy(), native_get_ad_data()  (native)
 *
 * All signals keep their original names (already unique per ad type).
 * on_paid_event includes ad_type as the first parameter for routing.
 */
class FruitMergeAdMobPlugin(godot: Godot) : GodotPlugin(godot) {

    companion object {
        private const val TAG = "FruitMergeAdMob"
    }

    private var activity: Activity? = null

    // Init state
    private var isInitialized = false

    // Banner state
    private val adViews = java.util.concurrent.ConcurrentHashMap<Int, AdView>()
    private var bannerNextUid = 1

    // Rewarded state
    private val rewardedAds = java.util.concurrent.ConcurrentHashMap<Int, RewardedAd>()
    private var rewardedNextUid = 1

    // Interstitial state
    private val interstitialAds = java.util.concurrent.ConcurrentHashMap<Int, InterstitialAd>()
    private var interstitialNextUid = 1

    // Rewarded Interstitial state
    private val rewardedInterstitialAds = java.util.concurrent.ConcurrentHashMap<Int, RewardedInterstitialAd>()
    private var rewardedInterstitialNextUid = 1

    // Native state
    private val nativeAds = java.util.concurrent.ConcurrentHashMap<Int, NativeAd>()
    private var nativeNextUid = 1

    // =========================================================================
    // Plugin Registration
    // =========================================================================

    override fun getPluginName(): String = "FruitMergeAdMob"

    override fun getPluginSignals(): Set<SignalInfo> = setOf(
        // Init
        SignalInfo("on_initialization_complete", java.util.Hashtable::class.java),

        // Banner
        SignalInfo("on_ad_clicked", Int::class.javaObjectType),
        SignalInfo("on_ad_closed", Int::class.javaObjectType),
        SignalInfo("on_ad_failed_to_load", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_ad_impression", Int::class.javaObjectType),
        SignalInfo("on_ad_loaded", Int::class.javaObjectType),
        SignalInfo("on_ad_opened", Int::class.javaObjectType),

        // Rewarded
        SignalInfo("on_rewarded_ad_loaded", Int::class.javaObjectType),
        SignalInfo("on_rewarded_ad_failed_to_load", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_ad_user_earned_reward", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_ad_clicked", Int::class.javaObjectType),
        SignalInfo("on_rewarded_ad_dismissed_full_screen_content", Int::class.javaObjectType),
        SignalInfo("on_rewarded_ad_failed_to_show_full_screen_content", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_ad_impression", Int::class.javaObjectType),
        SignalInfo("on_rewarded_ad_showed_full_screen_content", Int::class.javaObjectType),

        // Interstitial
        SignalInfo("on_interstitial_ad_loaded", Int::class.javaObjectType),
        SignalInfo("on_interstitial_ad_failed_to_load", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_interstitial_ad_clicked", Int::class.javaObjectType),
        SignalInfo("on_interstitial_ad_dismissed_full_screen_content", Int::class.javaObjectType),
        SignalInfo("on_interstitial_ad_failed_to_show_full_screen_content", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_interstitial_ad_impression", Int::class.javaObjectType),
        SignalInfo("on_interstitial_ad_showed_full_screen_content", Int::class.javaObjectType),

        // Rewarded Interstitial
        SignalInfo("on_rewarded_interstitial_ad_loaded", Int::class.javaObjectType),
        SignalInfo("on_rewarded_interstitial_ad_failed_to_load", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_interstitial_ad_user_earned_reward", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_interstitial_ad_clicked", Int::class.javaObjectType),
        SignalInfo("on_rewarded_interstitial_ad_dismissed_full_screen_content", Int::class.javaObjectType),
        SignalInfo("on_rewarded_interstitial_ad_failed_to_show_full_screen_content", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_rewarded_interstitial_ad_impression", Int::class.javaObjectType),
        SignalInfo("on_rewarded_interstitial_ad_showed_full_screen_content", Int::class.javaObjectType),

        // Native
        SignalInfo("on_native_ad_loaded", Int::class.javaObjectType),
        SignalInfo("on_native_ad_failed_to_load", Int::class.javaObjectType, java.util.Hashtable::class.java),
        SignalInfo("on_native_ad_clicked", Int::class.javaObjectType),
        SignalInfo("on_native_ad_impression", Int::class.javaObjectType),

        // Unified paid event: (ad_type, uid, value_micros, currency_code, precision)
        SignalInfo(
            "on_paid_event",
            String::class.java, Int::class.javaObjectType,
            Long::class.javaObjectType, String::class.java, Int::class.javaObjectType
        ),
    )

    override fun onMainCreate(activity: Activity?): View? {
        this.activity = activity
        Log.i(TAG, "FruitMergeAdMob consolidated plugin created")
        // Auto-initialize the Google Mobile Ads SDK
        initialize()
        return null
    }

    override fun onMainDestroy() {
        for ((_, adView) in adViews) { adView.destroy() }
        adViews.clear()
        rewardedAds.clear()
        interstitialAds.clear()
        rewardedInterstitialAds.clear()
        for ((_, nativeAd) in nativeAds) { nativeAd.destroy() }
        nativeAds.clear()
    }

    // =========================================================================
    // INITIALIZATION (no prefix — unique names already)
    // =========================================================================

    @UsedByGodot
    fun initialize() {
        val ctx = activity?.applicationContext ?: run {
            Log.e(TAG, "Cannot initialize — activity is null")
            emitSignal("on_initialization_complete", hashMapOf<String, Any>())
            return
        }
        if (isInitialized) {
            Log.w(TAG, "MobileAds already initialized, fetching current status")
            emitSignal("on_initialization_complete", getInitializationStatusDict())
            return
        }
        MobileAds.initialize(ctx) { initializationStatus ->
            isInitialized = true
            Log.i(TAG, "MobileAds initialized successfully")
            emitSignal("on_initialization_complete", getInitializationStatusDict())
        }
    }

    @UsedByGodot
    fun set_request_configuration(configDict: java.util.Hashtable<Any, Any>, testDeviceIds: Array<String>) {
        val builder = RequestConfiguration.Builder()
        val maxRating = configDict["max_ad_content_rating"] as? String
        if (maxRating != null) builder.setMaxAdContentRating(maxRating)
        val tagForChild = configDict["tag_for_child_directed_treatment"] as? Int
        if (tagForChild != null) builder.setTagForChildDirectedTreatment(tagForChild)
        val tagForUnderAge = configDict["tag_for_under_age_of_consent"] as? Int
        if (tagForUnderAge != null) builder.setTagForUnderAgeOfConsent(tagForUnderAge)
        if (testDeviceIds.isNotEmpty()) builder.setTestDeviceIds(testDeviceIds.toList())
        MobileAds.setRequestConfiguration(builder.build())
        Log.d(TAG, "Request configuration set (test_devices=${testDeviceIds.size})")
    }

    @UsedByGodot
    fun get_initialization_status(): java.util.Hashtable<Any, Any> = getInitializationStatusDict()

    @UsedByGodot
    fun set_app_volume(volume: Float) {
        MobileAds.setAppVolume(volume.coerceIn(0f, 1f))
    }

    @UsedByGodot
    fun set_app_muted(muted: Boolean) {
        MobileAds.setAppMuted(muted)
    }

    // =========================================================================
    // BANNER ADS (banner_ prefix)
    // =========================================================================

    @UsedByGodot
    fun banner_create(adViewDict: java.util.Hashtable<Any, Any>): Int {
        val act = activity ?: run {
            Log.e(TAG, "Cannot create banner — activity is null")
            return -1
        }
        val uid = bannerNextUid++
        val adUnitId = adViewDict["ad_unit_id"] as? String ?: ""
        val adPosition = adViewDict["ad_position"] as? Int ?: 0
        val adSizeDict = adViewDict["ad_size"] as? java.util.Hashtable<Any, Any>
        val width = adSizeDict?.get("width") as? Int ?: AdSize.FULL_WIDTH
        val height = adSizeDict?.get("height") as? Int ?: 50

        val adView = AdView(act)
        adView.setAdUnitId(adUnitId)
        adView.setAdSize(AdSize(width, height))

        adView.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
            AdRevenueHelper.logPaidEvent("banner", uid, adValue)
            emitSignal("on_paid_event", "banner", uid, adValue.valueMicros, adValue.currencyCode, adValue.precisionType)
        }

        adView.adListener = object : AdListener() {
            override fun onAdClicked() { emitSignal("on_ad_clicked", uid) }
            override fun onAdClosed() { emitSignal("on_ad_closed", uid) }
            override fun onAdFailedToLoad(error: LoadAdError) {
                emitSignal("on_ad_failed_to_load", uid, createLoadAdErrorDict(error))
            }
            override fun onAdImpression() { emitSignal("on_ad_impression", uid) }
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded (uid=$uid)")
                emitSignal("on_ad_loaded", uid)
            }
            override fun onAdOpened() { emitSignal("on_ad_opened", uid) }
        }

        act.runOnUiThread {
            val contentView = act.findViewById<ViewGroup>(android.R.id.content)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = if (adPosition == 0) Gravity.BOTTOM else Gravity.TOP
            contentView.addView(adView, params)
        }

        adViews[uid] = adView
        Log.d(TAG, "Created banner ad uid=$uid unit=$adUnitId pos=$adPosition size=${width}x${height}")
        return uid
    }

    @UsedByGodot
    fun banner_load_ad(uid: Int, requestDict: java.util.Hashtable<Any, Any>, keywords: Array<String>) {
        val adView = adViews[uid] ?: run { Log.w(TAG, "banner_load_ad: unknown uid=$uid"); return }
        adView.loadAd(buildAdRequest(requestDict, keywords))
    }

    @UsedByGodot
    fun banner_destroy(uid: Int) {
        val adView = adViews.remove(uid) ?: return
        activity?.runOnUiThread {
            (adView.parent as? ViewGroup)?.removeView(adView)
            adView.destroy()
        }
        Log.d(TAG, "Destroyed banner ad uid=$uid")
    }

    @UsedByGodot
    fun banner_hide(uid: Int) {
        adViews[uid]?.let { activity?.runOnUiThread { it.visibility = View.GONE } }
    }

    @UsedByGodot
    fun banner_show(uid: Int) {
        adViews[uid]?.let { activity?.runOnUiThread { it.visibility = View.VISIBLE } }
    }

    @UsedByGodot
    fun banner_get_width(uid: Int): Int = adViews[uid]?.adSize?.width ?: -1

    @UsedByGodot
    fun banner_get_height(uid: Int): Int = adViews[uid]?.adSize?.height ?: -1

    @UsedByGodot
    fun banner_get_width_in_pixels(uid: Int): Int {
        val adView = adViews[uid] ?: return -1
        val act = activity ?: return -1
        return adView.adSize?.getWidthInPixels(act) ?: -1
    }

    @UsedByGodot
    fun banner_get_height_in_pixels(uid: Int): Int {
        val adView = adViews[uid] ?: return -1
        val act = activity ?: return -1
        return adView.adSize?.getHeightInPixels(act) ?: -1
    }

    // =========================================================================
    // REWARDED ADS (rewarded_ prefix)
    // =========================================================================

    @UsedByGodot
    fun rewarded_create(): Int {
        val uid = rewardedNextUid++
        Log.d(TAG, "Created rewarded ad slot uid=$uid")
        return uid
    }

    @UsedByGodot
    fun rewarded_load(adUnitId: String, requestDict: java.util.Hashtable<Any, Any>, keywords: Array<String>, uid: Int) {
        val act = activity ?: run {
            Log.e(TAG, "Cannot load rewarded ad — activity is null")
            emitSignal("on_rewarded_ad_failed_to_load", uid, createErrorDict(-1, "Activity is null", ""))
            return
        }
        RewardedAd.load(act, adUnitId, buildAdRequest(requestDict, keywords), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Rewarded ad failed to load uid=$uid: ${error.message}")
                emitSignal("on_rewarded_ad_failed_to_load", uid, createLoadAdErrorDict(error))
            }
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded ad loaded uid=$uid")
                ad.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
                    AdRevenueHelper.logPaidEvent("rewarded", uid, adValue)
                    emitSignal("on_paid_event", "rewarded", uid, adValue.valueMicros, adValue.currencyCode, adValue.precisionType)
                }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() { emitSignal("on_rewarded_ad_clicked", uid) }
                    override fun onAdDismissedFullScreenContent() {
                        emitSignal("on_rewarded_ad_dismissed_full_screen_content", uid)
                        rewardedAds.remove(uid)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Rewarded ad failed to show uid=$uid: ${error.message}")
                        emitSignal("on_rewarded_ad_failed_to_show_full_screen_content", uid, createAdErrorDict(error))
                        rewardedAds.remove(uid)
                    }
                    override fun onAdImpression() { emitSignal("on_rewarded_ad_impression", uid) }
                    override fun onAdShowedFullScreenContent() { emitSignal("on_rewarded_ad_showed_full_screen_content", uid) }
                }
                rewardedAds[uid] = ad
                emitSignal("on_rewarded_ad_loaded", uid)
            }
        })
    }

    @UsedByGodot
    fun rewarded_show(uid: Int) {
        val ad = rewardedAds[uid] ?: run { Log.w(TAG, "rewarded_show: no ad for uid=$uid"); return }
        val act = activity ?: return
        act.runOnUiThread {
            ad.show(act, OnUserEarnedRewardListener { rewardItem ->
                val rewardDict = java.util.Hashtable<Any, Any>()
                rewardDict["type"] = rewardItem.type
                rewardDict["amount"] = rewardItem.amount
                Log.d(TAG, "User earned reward uid=$uid: type=${rewardItem.type} amount=${rewardItem.amount}")
                emitSignal("on_rewarded_ad_user_earned_reward", uid, rewardDict)
            })
        }
    }

    @UsedByGodot
    fun rewarded_destroy(uid: Int) {
        rewardedAds.remove(uid)
        Log.d(TAG, "Destroyed rewarded ad uid=$uid")
    }

    @UsedByGodot
    fun rewarded_set_server_side_verification_options(uid: Int, optionsDict: java.util.Hashtable<Any, Any>) {
        val ad = rewardedAds[uid] ?: return
        val builder = ServerSideVerificationOptions.Builder()
        (optionsDict["custom_data"] as? String)?.let { builder.setCustomData(it) }
        (optionsDict["user_id"] as? String)?.let { builder.setUserId(it) }
        ad.setServerSideVerificationOptions(builder.build())
    }

    // =========================================================================
    // INTERSTITIAL ADS (interstitial_ prefix)
    // =========================================================================

    @UsedByGodot
    fun interstitial_create(): Int {
        val uid = interstitialNextUid++
        Log.d(TAG, "Created interstitial ad slot uid=$uid")
        return uid
    }

    @UsedByGodot
    fun interstitial_load(adUnitId: String, requestDict: java.util.Hashtable<Any, Any>, keywords: Array<String>, uid: Int) {
        val act = activity ?: run {
            Log.e(TAG, "Cannot load interstitial ad — activity is null")
            emitSignal("on_interstitial_ad_failed_to_load", uid, createErrorDict(-1, "Activity is null", ""))
            return
        }
        InterstitialAd.load(act, adUnitId, buildAdRequest(requestDict, keywords), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Interstitial ad failed to load uid=$uid: ${error.message}")
                emitSignal("on_interstitial_ad_failed_to_load", uid, createLoadAdErrorDict(error))
            }
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial ad loaded uid=$uid")
                ad.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
                    AdRevenueHelper.logPaidEvent("interstitial", uid, adValue)
                    emitSignal("on_paid_event", "interstitial", uid, adValue.valueMicros, adValue.currencyCode, adValue.precisionType)
                }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() { emitSignal("on_interstitial_ad_clicked", uid) }
                    override fun onAdDismissedFullScreenContent() {
                        emitSignal("on_interstitial_ad_dismissed_full_screen_content", uid)
                        interstitialAds.remove(uid)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Interstitial ad failed to show uid=$uid: ${error.message}")
                        emitSignal("on_interstitial_ad_failed_to_show_full_screen_content", uid, createAdErrorDict(error))
                        interstitialAds.remove(uid)
                    }
                    override fun onAdImpression() { emitSignal("on_interstitial_ad_impression", uid) }
                    override fun onAdShowedFullScreenContent() { emitSignal("on_interstitial_ad_showed_full_screen_content", uid) }
                }
                interstitialAds[uid] = ad
                emitSignal("on_interstitial_ad_loaded", uid)
            }
        })
    }

    @UsedByGodot
    fun interstitial_show(uid: Int) {
        val ad = interstitialAds[uid] ?: run { Log.w(TAG, "interstitial_show: no ad for uid=$uid"); return }
        val act = activity ?: return
        act.runOnUiThread { ad.show(act) }
    }

    @UsedByGodot
    fun interstitial_destroy(uid: Int) {
        interstitialAds.remove(uid)
        Log.d(TAG, "Destroyed interstitial ad uid=$uid")
    }

    // =========================================================================
    // REWARDED INTERSTITIAL ADS (rewarded_interstitial_ prefix)
    // =========================================================================

    @UsedByGodot
    fun rewarded_interstitial_create(): Int {
        val uid = rewardedInterstitialNextUid++
        Log.d(TAG, "Created rewarded interstitial ad slot uid=$uid")
        return uid
    }

    @UsedByGodot
    fun rewarded_interstitial_load(adUnitId: String, requestDict: java.util.Hashtable<Any, Any>, keywords: Array<String>, uid: Int) {
        val act = activity ?: run {
            Log.e(TAG, "Cannot load rewarded interstitial ad — activity is null")
            emitSignal("on_rewarded_interstitial_ad_failed_to_load", uid, createErrorDict(-1, "Activity is null", ""))
            return
        }
        RewardedInterstitialAd.load(act, adUnitId, buildAdRequest(requestDict, keywords), object : RewardedInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Rewarded interstitial ad failed to load uid=$uid: ${error.message}")
                emitSignal("on_rewarded_interstitial_ad_failed_to_load", uid, createLoadAdErrorDict(error))
            }
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                Log.d(TAG, "Rewarded interstitial ad loaded uid=$uid")
                ad.onPaidEventListener = com.google.android.gms.ads.OnPaidEventListener { adValue ->
                    AdRevenueHelper.logPaidEvent("rewarded_interstitial", uid, adValue)
                    emitSignal("on_paid_event", "rewarded_interstitial", uid, adValue.valueMicros, adValue.currencyCode, adValue.precisionType)
                }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdClicked() { emitSignal("on_rewarded_interstitial_ad_clicked", uid) }
                    override fun onAdDismissedFullScreenContent() {
                        emitSignal("on_rewarded_interstitial_ad_dismissed_full_screen_content", uid)
                        rewardedInterstitialAds.remove(uid)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.w(TAG, "Rewarded interstitial ad failed to show uid=$uid: ${error.message}")
                        emitSignal("on_rewarded_interstitial_ad_failed_to_show_full_screen_content", uid, createAdErrorDict(error))
                        rewardedInterstitialAds.remove(uid)
                    }
                    override fun onAdImpression() { emitSignal("on_rewarded_interstitial_ad_impression", uid) }
                    override fun onAdShowedFullScreenContent() { emitSignal("on_rewarded_interstitial_ad_showed_full_screen_content", uid) }
                }
                rewardedInterstitialAds[uid] = ad
                emitSignal("on_rewarded_interstitial_ad_loaded", uid)
            }
        })
    }

    @UsedByGodot
    fun rewarded_interstitial_show(uid: Int) {
        val ad = rewardedInterstitialAds[uid] ?: run { Log.w(TAG, "rewarded_interstitial_show: no ad for uid=$uid"); return }
        val act = activity ?: return
        act.runOnUiThread {
            ad.show(act, OnUserEarnedRewardListener { rewardItem ->
                val rewardDict = java.util.Hashtable<Any, Any>()
                rewardDict["type"] = rewardItem.type
                rewardDict["amount"] = rewardItem.amount
                Log.d(TAG, "User earned rewarded interstitial reward uid=$uid: type=${rewardItem.type} amount=${rewardItem.amount}")
                emitSignal("on_rewarded_interstitial_ad_user_earned_reward", uid, rewardDict)
            })
        }
    }

    @UsedByGodot
    fun rewarded_interstitial_destroy(uid: Int) {
        rewardedInterstitialAds.remove(uid)
        Log.d(TAG, "Destroyed rewarded interstitial ad uid=$uid")
    }

    @UsedByGodot
    fun rewarded_interstitial_set_server_side_verification_options(uid: Int, optionsDict: java.util.Hashtable<Any, Any>) {
        val ad = rewardedInterstitialAds[uid] ?: return
        val builder = ServerSideVerificationOptions.Builder()
        (optionsDict["custom_data"] as? String)?.let { builder.setCustomData(it) }
        (optionsDict["user_id"] as? String)?.let { builder.setUserId(it) }
        ad.setServerSideVerificationOptions(builder.build())
    }

    // =========================================================================
    // NATIVE ADS (native_ prefix)
    // =========================================================================

    @UsedByGodot
    fun native_create(): Int {
        val uid = nativeNextUid++
        Log.d(TAG, "Created native ad slot uid=$uid")
        return uid
    }

    @UsedByGodot
    fun native_load(adUnitId: String, requestDict: java.util.Hashtable<Any, Any>, keywords: Array<String>, uid: Int) {
        val act = activity ?: run {
            Log.e(TAG, "Cannot load native ad — activity is null")
            emitSignal("on_native_ad_failed_to_load", uid, createErrorDict(-1, "Activity is null", ""))
            return
        }
        val adLoader = AdLoader.Builder(act, adUnitId)
            .forNativeAd { nativeAd: NativeAd ->
                Log.d(TAG, "Native ad loaded uid=$uid")
                nativeAds[uid]?.destroy()
                nativeAds[uid] = nativeAd
                emitSignal("on_native_ad_loaded", uid)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load uid=$uid: ${error.message}")
                    emitSignal("on_native_ad_failed_to_load", uid, createLoadAdErrorDict(error))
                }
                override fun onAdClicked() { emitSignal("on_native_ad_clicked", uid) }
                override fun onAdImpression() { emitSignal("on_native_ad_impression", uid) }
            })
            .build()
        adLoader.loadAd(buildAdRequest(requestDict, keywords))
        Log.d(TAG, "Loading native ad uid=$uid unit=$adUnitId")
    }

    @UsedByGodot
    fun native_get_ad_data(uid: Int): java.util.Hashtable<Any, Any> {
        val nativeAd = nativeAds[uid] ?: run {
            Log.w(TAG, "native_get_ad_data: no native ad for uid=$uid")
            return java.util.Hashtable()
        }
        val dict = java.util.Hashtable<Any, Any>()
        dict["headline"] = nativeAd.headline ?: ""
        dict["body"] = nativeAd.body ?: ""
        dict["call_to_action"] = nativeAd.callToAction ?: ""
        dict["advertiser"] = nativeAd.advertiser ?: ""
        dict["store"] = nativeAd.store ?: ""
        dict["price"] = nativeAd.price ?: ""
        nativeAd.starRating?.let { dict["star_rating"] = it.toDouble() }

        val icon = nativeAd.icon
        if (icon != null) {
            val iconUri = icon.getUri()
            if (iconUri != null) dict["icon_uri"] = iconUri.toString()
        }

        val mediaContent = nativeAd.mediaContent
        if (mediaContent != null) {
            dict["has_video_content"] = mediaContent.hasVideoContent()
            dict["media_aspect_ratio"] = mediaContent.aspectRatio
            dict["duration"] = mediaContent.duration
        }

        val images = nativeAd.images
        if (images != null && images.isNotEmpty()) {
            dict["image_count"] = images.size
        }

        return dict
    }

    @UsedByGodot
    fun native_destroy(uid: Int) {
        nativeAds.remove(uid)?.destroy()
        Log.d(TAG, "Destroyed native ad uid=$uid")
    }

    // =========================================================================
    // SHARED HELPERS
    // =========================================================================

    private fun getInitializationStatusDict(): java.util.Hashtable<Any, Any> {
        val result = java.util.Hashtable<Any, Any>()
        val status = MobileAds.getInitializationStatus() ?: return result
        for (entry in status.adapterStatusMap.entries) {
            val adapterDict = java.util.Hashtable<Any, Any>()
            adapterDict["initialization_state"] = entry.value.initializationState.ordinal
            adapterDict["description"] = entry.value.description
            adapterDict["latency"] = entry.value.latency
            result[entry.key] = adapterDict
        }
        return result
    }

    private fun buildAdRequest(dict: java.util.Hashtable<Any, Any>, keywords: Array<String>): AdRequest {
        val builder = AdRequest.Builder()
        if (keywords.isNotEmpty()) {
            for (kw in keywords) builder.addKeyword(kw)
        }
        return builder.build()
    }

    private fun createLoadAdErrorDict(error: LoadAdError): java.util.Hashtable<Any, Any> {
        val dict = java.util.Hashtable<Any, Any>()
        dict["code"] = error.code
        dict["message"] = error.message
        dict["domain"] = error.domain
        return dict
    }

    private fun createAdErrorDict(error: AdError): java.util.Hashtable<Any, Any> {
        val dict = java.util.Hashtable<Any, Any>()
        dict["code"] = error.code
        dict["message"] = error.message
        dict["domain"] = error.domain
        return dict
    }

    private fun createErrorDict(code: Int, message: String, domain: String): java.util.Hashtable<Any, Any> {
        val dict = java.util.Hashtable<Any, Any>()
        dict["code"] = code
        dict["message"] = message
        dict["domain"] = domain
        return dict
    }
}
