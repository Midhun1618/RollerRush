
package com.voxcom.rollerrush.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class CompletedRun(
    val runId: String,
    val score: Int,
    val distanceMeters: Int,
    val coins: Int,
    val skateId: String,
    val completedAtMillis: Long
)

data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val bestDistance: Long,
    val bestSkateId: String
)

class FirebaseGameService(
    private val preferences: GamePreferences
) {

    private var pendingSyncInProgress = false

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    val currentUid: String?
        get() = auth.currentUser?.uid

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous == true

    val isGoogleLinked: Boolean
        get() = auth.currentUser?.providerData?.any {
            it.providerId == "google.com"
        } == true

    var displayName: String? = null
        private set

    // ------------------------------------------------------------
    // USERNAME GENERATION
    // ------------------------------------------------------------

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

    init {
        database.child(".info/connected")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected =
                        snapshot.getValue(Boolean::class.java) == true

                    if (connected && currentUid != null) {
                        syncPendingRuns()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(
                        "RollerRushFirebase",
                        "Connection listener cancelled: ${error.message}"
                    )
                }
            })
    }

    // ------------------------------------------------------------
    // AUTHENTICATION AND PLAYER PROFILE
    // ------------------------------------------------------------

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

                    // Publish an existing player's best record.
                    refreshOwnLeaderboardEntry(uid)

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
                        onComplete(false, error.message)
                        return
                    }

                    if (!committed) {
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

                    if (!actualName.equals(username, ignoreCase = true)) {
                        releaseUsername(username, uid)
                    }

                    displayName = actualName
                    onComplete(true, null)
                }
            }, false)
    }

    // ------------------------------------------------------------
    // CHANGE USERNAME
    // ------------------------------------------------------------

    fun changeUsername(
        requestedName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val uid = currentUid

        if (uid == null) {
            onComplete(false, "Player is not signed in.")
            return
        }

        val newName = requestedName.trim()

        if (!Regex("^[A-Za-z0-9_]{3,16}$").matches(newName)) {
            onComplete(
                false,
                "Use 3–16 letters, numbers or underscores."
            )
            return
        }

        val oldName = displayName

        if (oldName.equals(newName, ignoreCase = true)) {
            onComplete(true, null)
            return
        }

        val newKey = newName.lowercase(Locale.ROOT)

        database.child("usernames")
            .child(newKey)
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
                        onComplete(false, error.message)
                        return
                    }

                    if (!committed) {
                        onComplete(false, "That username is already taken.")
                        return
                    }

                    val updates = mapOf<String, Any>(
                        "displayName" to newName,
                        "username" to newName
                    )

                    database.child("users")
                        .child(uid)
                        .updateChildren(updates)
                        .addOnSuccessListener {

                            displayName = newName

                            // Update the public leaderboard name too.
                            database.child("leaderboard")
                                .child(uid)
                                .child("displayName")
                                .setValue(newName)

                            if (!oldName.isNullOrBlank()) {
                                val oldKey =
                                    oldName.lowercase(Locale.ROOT)

                                if (oldKey != newKey) {
                                    releaseUsername(oldName, uid)
                                }
                            }

                            onComplete(true, null)
                        }
                        .addOnFailureListener { updateError ->

                            releaseUsername(newName, uid)

                            onComplete(
                                false,
                                updateError.localizedMessage
                                    ?: "Could not update username."
                            )
                        }
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

    // ------------------------------------------------------------
    // LEADERBOARD
    // ------------------------------------------------------------

    /**
     * Reads the player's private profile and publishes only the
     * public leaderboard information.
     */
    private fun refreshOwnLeaderboardEntry(
        uid: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        database.child("users")
            .child(uid)
            .get()
            .addOnSuccessListener { profile ->

                val distance =
                    (profile.child("bestDistance").value as? Number)
                        ?.toLong() ?: 0L

                if (distance <= 0L) {
                    onComplete(true, null)
                    return@addOnSuccessListener
                }

                val publicEntry = mapOf(
                    "displayName" to (
                            profile.child("displayName")
                                .getValue(String::class.java) ?: "RUSHER"
                            ),
                    "bestDistance" to distance,
                    "bestSkateId" to (
                            profile.child("bestSkateId")
                                .getValue(String::class.java) ?: "skate_basic"
                            ),
                    "updatedAtMillis" to System.currentTimeMillis()
                )

                database.child("leaderboard")
                    .child(uid)
                    .setValue(publicEntry)
                    .addOnSuccessListener {
                        onComplete(true, null)
                    }
                    .addOnFailureListener { error ->
                        onComplete(
                            false,
                            error.localizedMessage
                                ?: "Could not publish leaderboard entry."
                        )
                    }
            }
            .addOnFailureListener { error ->
                onComplete(
                    false,
                    error.localizedMessage ?: "Could not read player profile."
                )
            }
    }

    /**
     * Fetches the five highest personal-best distances.
     */
    fun fetchTopLeaderboard(
        onComplete: (List<LeaderboardEntry>, String?) -> Unit
    ) {
        database.child("leaderboard")
            .orderByChild("bestDistance")
            .limitToLast(5)
            .get()
            .addOnSuccessListener { snapshot ->

                val entries = snapshot.children.mapNotNull { child ->

                    val uid = child.key ?: return@mapNotNull null

                    val distance =
                        (child.child("bestDistance").value as? Number)
                            ?.toLong() ?: 0L

                    if (distance <= 0L) {
                        return@mapNotNull null
                    }

                    LeaderboardEntry(
                        uid = uid,
                        displayName = child.child("displayName")
                            .getValue(String::class.java) ?: "RUSHER",
                        bestDistance = distance,
                        bestSkateId = child.child("bestSkateId")
                            .getValue(String::class.java) ?: "skate_basic"
                    )
                }.sortedByDescending { it.bestDistance }

                onComplete(entries, null)
            }
            .addOnFailureListener { error ->
                onComplete(
                    emptyList(),
                    error.localizedMessage ?: "Could not load leaderboard."
                )
            }
    }

    // ------------------------------------------------------------
    // COMPLETED RUNS AND OFFLINE SYNC
    // ------------------------------------------------------------

    private fun readPendingRuns(): MutableList<CompletedRun> {
        val result = mutableListOf<CompletedRun>()

        try {
            val array = JSONArray(preferences.getPendingRunsJson())

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val runId = item.optString("runId")

                if (runId.isBlank()) continue

                result.add(
                    CompletedRun(
                        runId = runId,
                        score = item.optInt("score", 0),
                        distanceMeters = item.optInt("distanceMeters", 0),
                        coins = item.optInt("coins", 0),
                        skateId = item.optString(
                            "skateId",
                            "skate_basic"
                        ),
                        completedAtMillis = item.optLong(
                            "completedAtMillis",
                            0L
                        )
                    )
                )
            }
        } catch (error: Exception) {
            Log.e(
                "RollerRushFirebase",
                "Could not read pending runs",
                error
            )
        }

        return result
    }

    private fun savePendingRuns(runs: List<CompletedRun>) {
        val array = JSONArray()

        runs.forEach { run ->
            array.put(
                JSONObject().apply {
                    put("runId", run.runId)
                    put("score", run.score)
                    put("distanceMeters", run.distanceMeters)
                    put("coins", run.coins)
                    put("skateId", run.skateId)
                    put("completedAtMillis", run.completedAtMillis)
                }
            )
        }

        preferences.setPendingRunsJson(array.toString())
    }

    private fun enqueueRun(run: CompletedRun) {
        val runs = readPendingRuns()

        if (runs.any { it.runId == run.runId }) return

        runs.add(run)
        savePendingRuns(runs)
    }

    /**
     * Saves a completed run locally first, then attempts to sync.
     */
    fun submitCompletedRun(run: CompletedRun) {
        val safeRun = run.copy(
            score = run.score.coerceAtLeast(0),
            distanceMeters = run.distanceMeters.coerceAtLeast(0),
            coins = run.coins.coerceAtLeast(0),
            skateId = run.skateId.ifBlank { "skate_basic" }
        )

        enqueueRun(safeRun)

        if (currentUid == null) {
            initialize { success, error ->
                if (success) {
                    syncPendingRuns()
                } else {
                    Log.w(
                        "RollerRushFirebase",
                        "Run saved offline; sign-in/sync pending: $error"
                    )
                }
            }
        } else {
            syncPendingRuns()
        }
    }

    /**
     * Uploads queued runs sequentially.
     * A run is removed only after Firebase confirms its upload
     * and its public leaderboard entry has been updated.
     */
    fun syncPendingRuns() {
        if (pendingSyncInProgress) return

        val uid = currentUid ?: return
        val runs = readPendingRuns()

        if (runs.isEmpty()) return

        pendingSyncInProgress = true
        uploadNextRun(uid, runs, 0)
    }

    private fun uploadNextRun(
        uid: String,
        runs: List<CompletedRun>,
        index: Int
    ) {
        if (index >= runs.size) {
            pendingSyncInProgress = false

            if (readPendingRuns().isNotEmpty()) {
                syncPendingRuns()
            }

            return
        }

        val run = runs[index]

        saveRunToFirebase(uid, run) { success, error ->

            if (success) {
                val remainingRuns = readPendingRuns()
                    .filterNot { it.runId == run.runId }

                savePendingRuns(remainingRuns)

                uploadNextRun(uid, runs, index + 1)

            } else {
                pendingSyncInProgress = false

                Log.w(
                    "RollerRushFirebase",
                    "Run ${run.runId} remains queued: $error"
                )
            }
        }
    }

    /**
     * Records the run exactly once, updates lifetime statistics,
     * updates personal best, then refreshes the public leaderboard.
     */
    private fun saveRunToFirebase(
        uid: String,
        run: CompletedRun,
        onComplete: (Boolean, String?) -> Unit
    ) {
        database.child("users")
            .child(uid)
            .runTransaction(object : Transaction.Handler {

                override fun doTransaction(
                    currentData: MutableData
                ): Transaction.Result {

                    val runNode = currentData
                        .child("runs")
                        .child(run.runId)

                    // Avoid counting the same run more than once.
                    if (runNode.value != null) {
                        return Transaction.success(currentData)
                    }

                    val oldTotalDistance =
                        (currentData.child("totalDistance").value as? Number)
                            ?.toLong() ?: 0L

                    val oldTotalRuns =
                        (currentData.child("totalRuns").value as? Number)
                            ?.toLong() ?: 0L

                    val oldBestDistance =
                        (currentData.child("bestDistance").value as? Number)
                            ?.toLong() ?: 0L

                    val distance = run.distanceMeters.toLong()

                    runNode.child("score").value = run.score
                    runNode.child("distanceMeters").value =
                        run.distanceMeters
                    runNode.child("coins").value = run.coins
                    runNode.child("skateId").value = run.skateId
                    runNode.child("completedAtMillis").value =
                        run.completedAtMillis

                    currentData.child("totalDistance").value =
                        if (Long.MAX_VALUE - oldTotalDistance < distance) {
                            Long.MAX_VALUE
                        } else {
                            oldTotalDistance + distance
                        }

                    currentData.child("totalRuns").value =
                        if (oldTotalRuns == Long.MAX_VALUE) {
                            Long.MAX_VALUE
                        } else {
                            oldTotalRuns + 1L
                        }

                    if (distance > oldBestDistance) {
                        currentData.child("bestDistance").value = distance
                        currentData.child("bestSkateId").value = run.skateId
                    }

                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        onComplete(false, error.message)

                    } else if (!committed) {
                        onComplete(
                            false,
                            "Firebase did not commit the run."
                        )

                    } else {
                        // Publish the latest personal best to the
                        // public leaderboard before removing the queue item.
                        refreshOwnLeaderboardEntry(uid) { published, publishError ->
                            onComplete(published, publishError)
                        }
                    }
                }
            }, false)
    }
}
