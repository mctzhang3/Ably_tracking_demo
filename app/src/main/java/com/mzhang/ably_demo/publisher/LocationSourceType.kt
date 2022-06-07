package com.mzhang.ably_demo.publisher

import androidx.annotation.StringRes
import com.mzhang.ably_demo.R

enum class LocationSourceType(@StringRes val displayNameResourceId: Int) {
    PHONE(R.string.location_source_phone),
    ABLY_CHANNEL(R.string.location_source_ably_channel),
    S3_FILE(R.string.location_source_s3_file)
}
