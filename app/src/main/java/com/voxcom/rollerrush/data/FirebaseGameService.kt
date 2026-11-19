package com.voxcom.rollerrush.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import java.util.Locale

class FirebaseGameService {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    val currentUid: String?
        get() = auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    var displayName: String? = null
        private set

    private val prefixes = listOf(
        "Fire", "Neon", "Shadow", "Turbo", "Frost",
        "Cosmic", "Storm", "Pixel", "Blaze", "Thunder",
        "Rapid", "Hyper", "Ultra", "Nitro", "Phantom",
        "Mystic", "Arctic", "Solar", "Lunar", "Cyber",
        "Atomic", "Electric", "Savage", "Golden", "Silver",
        "Crimson", "Violet", "Toxic", "Rocket", "Aero",
        "Sonic", "Dark", "Wild", "Epic", "Nova",
        "Quantum", "Inferno", "Frozen", "Venom", "Royal",
        "Lucky", "Mighty", "Stealth", "Alpha", "Omega",
        "Velocity", "Mega", "TurboX", "Night", "Sky"
    )

    private val names = listOf(
        "Wolf", "Rider", "Skater", "Blaze", "Rocket",
        "Phantom", "Falcon", "Drift", "Tiger", "Dragon",
        "Eagle", "Viper", "Ranger", "Hunter", "Ninja",
        "Legend", "Warrior", "Comet", "Panda", "Fox",
        "Lion", "Hawk", "Racer", "Knight", "Rebel",
        "Spark", "Storm", "Ghost", "Cobra", "Phoenix",
        "Striker", "Dynamo", "Raptor", "Wizard", "Ace",
        "Dash", "Bolt", "Crusher", "Maverick", "Titan",
        "Cyclone", "Glider", "RocketX", "Arrow", "Bandit",
        "Orbit", "Chaser", "Rover", "Voyager", "Jumper"
    )

    private val suffixes = listOf(
        "07", "12", "16", "20", "24",
        "27", "30", "33", "36", "40",
        "42", "45", "48", "50", "52",
        "55", "58", "60", "63", "66",
        "69", "72", "75", "77", "80",
        "82", "85", "88", "90", "93",
        "95", "99", "101", "108", "111",
        "123", "131", "144", "150", "161",
        "171", "180", "202", "222", "247",
        "256", "303", "404", "777", "999"
    )

    fun initialize(
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val existingUser = auth.currentUser

        if (existingUser != null) {
            ensurePlayerProfile(existingUser.uid, onComplete)
            return
        }

        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid == null) {
                        onComplete(false, "Could not retrieve player UID")
                    } else {
                        ensurePlayerProfile(uid, onComplete)
                    }
                } else {
                    onComplete(
                        false,
                        task.exception?.localizedMessage
                            ?: "Anonymous authentication failed"
                    )
                }
            }
    }

    private fun generateUsername(): String {
        return prefixes.random() +
                names.random() +
                suffixes.random()
    }

    private fun ensurePlayerProfile(
        uid: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        database.child("users")
            .child(uid)
            .child("displayName")
            .get()
            .addOnSuccessListener { snapshot ->

                val existingName = snapshot.getValue(String::class.java)

                if (!existingName.isNullOrBlank()) {
                    displayName = existingName
                    onComplete(true, null)
                } else {
                    reserveUsername(uid, 0, onComplete)
                }
            }
            .addOnFailureListener { error ->
                onComplete(
                    false,
                    error.localizedMessage ?: "Could not load player profile"
                )
            }
    }

    private fun reserveUsername(
        uid: String,
        attempt: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (attempt >= 100) {
            onComplete(
                false,
                "Could not find an available username. Please try again."
            )
            return
        }

        val username = generateUsername()
        val usernameKey = username.lowercase(Locale.ROOT)

        database.child("usernames")
            .child(usernameKey)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    val owner = currentData.getValue(String::class.java)

                    if (owner == null || owner == uid) {
                        currentData.value = uid
                        return Transaction.success(currentData)
                    }

                    return Transaction.abort()
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        onComplete(
                            false,
                            error.message
                        )
                        return
                    }

                    if (!committed) {
                        // Another player already owns this username.
                        reserveUsername(uid, attempt + 1, onComplete)
                        return
                    }

                    createPlayerProfile(uid, username, onComplete)
                }
            }, false)
    }

    private fun createPlayerProfile(
        uid: String,
        username: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        database.child("users")
            .child(uid)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    val existingName =
                        currentData.child("displayName").value as? String

                    // Another device may already have created this profile.
                    if (!existingName.isNullOrBlank()) {
                        return Transaction.success(currentData)
                    }

                    currentData.child("displayName").value = username
                    currentData.child("username").value = username
                    currentData.child("createdAt").value = ServerValue.TIMESTAMP
                    currentData.child("bestDistance").value = 0
                    currentData.child("bestSkateId").value = "skate_basic"
                    currentData.child("totalDistance").value = 0
                    currentData.child("totalRuns").value = 0

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null || !committed) {
                        releaseUsername(username, uid)

                        onComplete(
                            false,
                            error?.message ?: "Could not create player profile"
                        )
                        return
                    }

                    val actualName = currentData
                        ?.child("displayName")
                        ?.getValue(String::class.java)

                    if (actualName.isNullOrBlank()) {
                        releaseUsername(username, uid)
                        onComplete(false, "Player profile has no display name")
                        return
                    }

                    // If another device won the profile creation race,
                    // release the unused username reservation.
                    if (!actualName.equals(username, ignoreCase = true)) {
                        releaseUsername(username, uid)
                    }

                    displayName = actualName
                    onComplete(true, null)
                }
            }, false)
    }

    private fun releaseUsername(username: String, uid: String) {
        val key = username.lowercase(Locale.ROOT)

        database.child("usernames")
            .child(key)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {
                    if (currentData.getValue(String::class.java) == uid) {
                        currentData.value = null
                        return Transaction.success(currentData)
                    }

                    return Transaction.abort()
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    // Cleanup is best-effort.
                }
            }, false)
    }
}
