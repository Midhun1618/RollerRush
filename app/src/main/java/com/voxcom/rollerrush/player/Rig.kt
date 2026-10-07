package com.voxcom.rollerrush.player

/**
 * Rig dimensions in WORLD UNITS. Shared by the sprite generator (AssetManager) and the
 * skeleton builder (Player) so art and skeleton always agree.
 *
 * Limb convention: bitmap is (thick x (len + thick)); pivot is the joint at the top
 * (thick/2, thick/2); the next joint sits `len` units further down the limb.
 */
object Rig {
    const val TORSO_W = 22f; const val TORSO_H = 34f
    const val TORSO_PIVOT_X = 11f; const val TORSO_PIVOT_Y = 31f      // pivot = hip joint
    const val NECK_Y = -29f                                           // neck, relative to hip
    const val SHOULDER_Y = -25f                                       // shoulders, relative to hip

    const val HEAD_W = 25f; const val HEAD_H = 24f
    const val HEAD_PIVOT_X = 11f; const val HEAD_PIVOT_Y = 22f       // pivot = neck

    const val HAIR_W = 24f; const val HAIR_H = 14f
    const val HAIR_PIVOT_X = 12f; const val HAIR_PIVOT_Y = 13f

    const val UPPER_ARM_LEN = 15f; const val UPPER_ARM_THICK = 6f
    const val FOREARM_LEN = 14f; const val FOREARM_THICK = 5f
    const val HAND_SIZE = 8f

    const val THIGH_LEN = 24f; const val THIGH_THICK = 12f
    const val SHIN_LEN = 25f; const val SHIN_THICK = 8f

    const val SKATE_W = 34f; const val SKATE_H = 17f
    const val SKATE_PIVOT_X = 9f; const val SKATE_PIVOT_Y = 2f       // pivot = ankle
    const val SKATE_DROP = SKATE_H - SKATE_PIVOT_Y                   // ankle -> wheel bottom
    const val SKATE_REACH = 13f                                       // ankle -> toe/heel, for pitch contact

    /** Gameplay hitboxes, relative to the player's ground contact point (bottomY). Deliberately NOT the full sprite. */
    const val BODY_BOX_HALF_W = 6f
    const val BODY_BOX_TOP = 86f       // upward from feet
    const val BODY_BOX_BOTTOM = 26f
    const val FEET_BOX_LEFT = 9f
    const val FEET_BOX_RIGHT = 11f
    const val FEET_BOX_TOP = 26f
    const val FEET_BOX_BOTTOM = 1f

    // While sliding, the head/torso are much lower and the upper body is tucked away.
    const val SLIDE_BODY_BOX_TOP = 50f
    const val SLIDE_BODY_BOX_BOTTOM = 12f
}
