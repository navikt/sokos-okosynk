package no.nav.sokos.okosynk.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class RegelverkConfigTest :
    FunSpec({
        test("Les inn mapping regel for OS") {
            val regelverkMap = RegelverkConfig.regelverkOSMap
            regelverkMap.shouldNotBeEmpty()
            regelverkMap.size shouldBe 84
            regelverkMap.forEach { (key, _) -> key shouldContain "_" }
        }

        test("Les inn mapping regel for UR") {
            val regelverkMap = RegelverkConfig.regelverkURMap
            regelverkMap.shouldNotBeEmpty()
            regelverkMap.size shouldBe 70
            regelverkMap.forEach { (key, _) -> key shouldContain "_" }
        }
    })
