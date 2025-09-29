package no.nav.sokos.okosynk.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

class RegelverkConfigTest :
    FunSpec({
        val keyPattern = Regex("^[A-Za-z0-9]+_[0-9]+$")

        test("Les inn mapping regel for OS") {
            val regelverkMap = RegelverkConfig.regelverkOSMap
            regelverkMap.shouldNotBeEmpty()
            regelverkMap.size shouldBe 86
            regelverkMap.forEach { (key, _) -> key shouldMatch keyPattern }
        }

        test("Les inn mapping regel for UR") {
            val regelverkMap = RegelverkConfig.regelverkURMap
            regelverkMap.shouldNotBeEmpty()
            regelverkMap.size shouldBe 70
            regelverkMap.forEach { (key, _) -> key shouldMatch keyPattern }
        }
    })
