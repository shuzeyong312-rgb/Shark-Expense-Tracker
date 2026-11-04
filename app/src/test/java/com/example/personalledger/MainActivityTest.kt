package com.example.personalledger

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test fun activityHasExpectedName() {
        assertEquals("com.example.personalledger.MainActivity", MainActivity::class.java.name)
    }
}
