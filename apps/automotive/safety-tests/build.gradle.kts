plugins { kotlin("jvm") version "2.0.21" }
repositories { mavenCentral() }
kotlin { jvmToolchain(17) }
sourceSets {
    main {
        kotlin.srcDir("../app/src/main/java")
        kotlin.include("**/SessionState.kt", "**/PlanPayload.kt", "**/NavigationPolicy.kt", "**/JourneyFlow.kt",
            "**/JourneyDrafts.kt", "**/VoiceInputSession.kt", "**/IntentReview.kt", "**/PlanExplanation.kt",
            "**/PlaceReview.kt", "**/PlanTiming.kt", "**/NavigationRequestState.kt", "**/NavigationHandoff.kt",
            "**/VoiceJourney.kt", "**/CockpitTheme.kt", "**/CockpitPresentation.kt", "**/SpokenPlan.kt",
            "**/SpokenCommand.kt", "**/SpokenExchange.kt", "**/LocationSetup.kt")
    }
    test { kotlin.srcDir("../app/src/test/java") }
}
dependencies { implementation("org.json:json:20240303"); testImplementation("junit:junit:4.13.2") }
