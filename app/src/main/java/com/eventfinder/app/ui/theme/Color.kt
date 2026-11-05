package com.eventfinder.app.ui.theme

import androidx.compose.ui.graphics.Color

// EventFinder South Africa brand palette
val BrandRed = Color(0xFFE03C31)
val BrandWhite = Color(0xFFFFFFFF)
val BrandGreen = Color(0xFF007749)
val BrandBlue = Color(0xFF001489)
val BrandGold = Color(0xFFFFB81C)
val BrandBlack = Color(0xFF000000)

// Supporting brand colours
val BrandGreenDark = Color(0xFF005638)
val BrandGreenGlow = Color(0xFF35A879)
val BrandAmber = BrandGold
val BrandAmberGlow = Color(0xFFFFD166)
val BrandNavy = BrandBlue
val BrandNavyGlow = Color(0xFF5265C4)
val BrandRedGlow = Color(0xFFFF746B)
val BrandPurple = Color(0xFF5E35B1)
val BrandPurpleGlow = Color(0xFF7E57C2)

// Light surfaces
val SurfaceOffWhite = Color(0xFFF6F8FA)
val SurfaceInk = Color(0xFF101722)
val SurfaceInkHigh = Color(0xFF172334)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceCardDark = Color(0xFF18263A)
val SurfaceBorder = Color(0xFFE7EBF0)
val SurfaceMuted = Color(0xFFF0F3F6)

// Decorative colours
val GlowGreen = Color(0x33007749)
val GlowAmber = Color(0x33FFB81C)
val GlowPurple = Color(0x335E35B1)

// Compatibility aliases used by existing screens
val Green = BrandGreen
val GreenDark = BrandGreenDark
val Amber = BrandGold
val Navy = BrandBlue
val Red = BrandRed
val OffWhite = SurfaceOffWhite
val Charcoal = SurfaceInk

// --- Screen accent & surface colours ---
// Named here (rather than hard-coded in individual screens) so the showcase
// screens draw from one palette. The bright accent green below is the "vibrant"
// accent used across cards and CTAs; it is intentionally lighter than the SA
// flag [BrandGreen] used for the core brand mark.
val AccentGreen = Color(0xFF00C98D)
val AccentGreenDark = Color(0xFF00A274)
val AccentGreenDeep = Color(0xFF06201A)
val ShowcaseNavy = Color(0xFF0B1929)
val ShowcaseNavyRaised = Color(0xFF1E2933)
val SlateDark = Color(0xFF334155)
val SlateGrey = Color(0xFF94A3B8)
val SlateGreyMuted = Color(0xFF94A3BB)

// Status / accent colours
val DangerRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val WarmAmber = Color(0xFFF4A261)
val AccentPurple = Color(0xFF7C4DFF)

// Third-party brand colours (fixed by each brand's guidelines)
val GoogleBlue = Color(0xFF4285F4)
val GoogleBlueDark = Color(0xFF1A73E8)
val WhatsAppGreen = Color(0xFF25D366)
val AndroidGreen = Color(0xFF3DDC84)

// Scrims for image/photo overlays
val ScrimStrong = Color(0xE6000000)
val ScrimSoft = Color(0x33000000)