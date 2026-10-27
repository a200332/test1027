package com.dailybrief.app.ui

import android.app.Application

/** Plain Application for Robolectric so WorkManager is not scheduled during UI tests. */
class TestApp : Application()
