package com.fruitmerge.admob

import android.util.Log
import com.google.android.gms.ads.AdValue

/**
 * Helper to extract revenue data from an AdValue for signal emission.
 *
 * Each ad plugin creates its OnPaidEventListener and calls emitSignal
 * directly (since emitSignal is protected in GodotPlugin).
 * This helper just extracts and logs the data.
 */
object AdRevenueHelper {

    private const val TAG = "FruitMergeAdMob"

    /**
     * Log the paid event data. Called from within each plugin's OnPaidEventListener.
     */
    fun logPaidEvent(adType: String, uid: Int, adValue: AdValue) {
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val precision = adValue.precisionType
        val revenueDollars = valueMicros / 1_000_000.0

        Log.d(TAG, "Paid event: type=$adType uid=$uid value=${valueMicros}u $currencyCode precision=$precision (\$${String.format("%.6f", revenueDollars)})")
    }
}
