package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CampNotification
import com.example.data.CampSettings
import com.example.data.CampUser
import com.example.data.CampAlbumPost
import com.example.data.AlbumComment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CampViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("camp_connect_prefs", Context.MODE_PRIVATE)

    // Current logged-in parent/child state
    private val _currentUser = MutableStateFlow<CampUser?>(null)
    val currentUser: StateFlow<CampUser?> = _currentUser.asStateFlow()

    // Live Camp Settings (Album Link, Daily Program, Dietary Program, Activities)
    private val _campSettings = MutableStateFlow(CampSettings())
    val campSettings: StateFlow<CampSettings> = _campSettings.asStateFlow()

    // Live notifications list
    private val _notifications = MutableStateFlow<List<CampNotification>>(emptyList())
    val notifications: StateFlow<List<CampNotification>> = _notifications.asStateFlow()

    // Live album posts feed (Facebook-style)
    private val _albumPosts = MutableStateFlow<List<CampAlbumPost>>(emptyList())
    val albumPosts: StateFlow<List<CampAlbumPost>> = _albumPosts.asStateFlow()

    // All registered users (for admin panel)
    private val _allUsers = MutableStateFlow<List<CampUser>>(emptyList())
    val allUsers: StateFlow<List<CampUser>> = _allUsers.asStateFlow()

    // Admin login state (session-based)
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Operation status messages/errors
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSavedUser()
        observeCampSettings()
        observeNotifications()
        observeAlbumPosts()
        observeAllUsers()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Try to load saved session from SharedPreferences
    private fun loadSavedUser() {
        val parentName = prefs.getString("parent_name", null)
        val childName = prefs.getString("child_name", null)
        if (!parentName.isNullOrBlank() && !childName.isNullOrBlank()) {
            val userId = generateUserId(parentName, childName)
            _isLoading.value = true
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val user = CampUser.fromMap(doc.data ?: emptyMap())
                        _currentUser.value = user
                        // Start a real-time listener for the active user's approval status
                        listenToActiveUser(userId)
                    } else {
                        // User no longer exists in DB, clear local session
                        clearLocalSession()
                    }
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _isLoading.value = false
                }
        }
    }

    // Listens to status updates (e.g. when Admin approves/rejects live)
    private fun listenToActiveUser(userId: String) {
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _currentUser.value = CampUser.fromMap(snapshot.data ?: emptyMap())
                }
            }
    }

    private fun observeCampSettings() {
        db.collection("config").document("camp_settings")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _campSettings.value = CampSettings.fromMap(snapshot.data ?: emptyMap())
                } else {
                    // Seed initial data if config doesn't exist yet
                    val initialSettings = CampSettings()
                    db.collection("config").document("camp_settings").set(initialSettings.toMap())
                }
            }
    }

    private fun observeNotifications() {
        db.collection("notifications")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val list = snapshots.documents.map { doc ->
                        CampNotification.fromMap(doc.id, doc.data ?: emptyMap())
                    }.sortedByDescending { it.createdAt }
                    _notifications.value = list
                }
            }
    }

    private fun observeAlbumPosts() {
        db.collection("album_posts")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val list = snapshots.documents.map { doc ->
                        CampAlbumPost.fromMap(doc.id, doc.data ?: emptyMap())
                    }.sortedByDescending { it.createdAt }
                    _albumPosts.value = list
                }
            }
    }

    private fun observeAllUsers() {
        db.collection("users")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val list = snapshots.documents.map { doc ->
                        CampUser.fromMap(doc.data ?: emptyMap())
                    }.sortedByDescending { it.registeredAt }
                    _allUsers.value = list
                }
            }
    }

    // Helper to generate consistent unique key
    private fun generateUserId(parentName: String, childName: String): String {
        val cleanParent = parentName.trim().lowercase()
        val cleanChild = childName.trim().lowercase()
        return "${cleanParent}_${cleanChild}".replace(" ", "_")
    }

    // Register a new Parent-Child profile
    fun registerUser(parentName: String, childName: String, onSuccess: () -> Unit) {
        if (parentName.isBlank() || childName.isBlank()) {
            _statusMessage.value = "يرجى ملء جميع الحقول المطلوبة"
            return
        }

        _isLoading.value = true
        val userId = generateUserId(parentName, childName)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Already registered, log in directly
                    val user = CampUser.fromMap(doc.data ?: emptyMap())
                    saveLocalSession(parentName, childName)
                    _currentUser.value = user
                    listenToActiveUser(userId)
                    _statusMessage.value = "الحساب موجود مسبقاً، تم تسجيل الدخول تلقائياً"
                    onSuccess()
                } else {
                    // Create new registration
                    val newUser = CampUser(
                        id = userId,
                        parentName = parentName.trim(),
                        childName = childName.trim(),
                        status = "pending"
                    )
                    db.collection("users").document(userId).set(newUser.toMap())
                        .addOnSuccessListener {
                            saveLocalSession(parentName, childName)
                            _currentUser.value = newUser
                            listenToActiveUser(userId)
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            _statusMessage.value = "خطأ في التسجيل: ${e.localizedMessage}"
                        }
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ في الاتصال: ${e.localizedMessage}"
                _isLoading.value = false
            }
    }

    // Quick login for registered users
    fun loginUser(parentName: String, childName: String, onSuccess: () -> Unit) {
        if (parentName.isBlank() || childName.isBlank()) {
            _statusMessage.value = "يرجى ملء جميع الحقول المطلوبة"
            return
        }

        _isLoading.value = true
        val userId = generateUserId(parentName, childName)

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = CampUser.fromMap(doc.data ?: emptyMap())
                    saveLocalSession(parentName, childName)
                    _currentUser.value = user
                    listenToActiveUser(userId)
                    onSuccess()
                } else {
                    _statusMessage.value = "الحساب غير موجود! يرجى التسجيل كولي جديد"
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ في الاتصال: ${e.localizedMessage}"
                _isLoading.value = false
            }
    }

    // Log in as administrator with a PIN code
    fun loginAdmin(pin: String, onSuccess: () -> Unit) {
        if (pin == "2026" || pin == "1234") { // Simple standard PIN codes for easy remembering
            _isAdmin.value = true
            onSuccess()
        } else {
            _statusMessage.value = "رمز المرور خاطئ! يرجى المحاولة مجدداً"
        }
    }

    fun logoutAdmin() {
        _isAdmin.value = false
    }

    fun logout() {
        clearLocalSession()
        _currentUser.value = null
    }

    private fun saveLocalSession(parentName: String, childName: String) {
        prefs.edit()
            .putString("parent_name", parentName.trim())
            .putString("child_name", childName.trim())
            .apply()
    }

    private fun clearLocalSession() {
        prefs.edit()
            .remove("parent_name")
            .remove("child_name")
            .apply()
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    // Approve a user registration
    fun approveUser(userId: String) {
        db.collection("users").document(userId).update("status", "approved")
            .addOnSuccessListener {
                _statusMessage.value = "تمت الموافقة على الحساب بنجاح"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ: ${e.localizedMessage}"
            }
    }

    // Reject a user registration
    fun rejectUser(userId: String) {
        db.collection("users").document(userId).update("status", "rejected")
            .addOnSuccessListener {
                _statusMessage.value = "تم رفض الحساب"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ: ${e.localizedMessage}"
            }
    }

    // Delete a user registration
    fun deleteUser(userId: String) {
        db.collection("users").document(userId).delete()
            .addOnSuccessListener {
                _statusMessage.value = "تم حذف الحساب بنجاح"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ: ${e.localizedMessage}"
            }
    }

    // Update programs and album URL
    fun updateCampContent(
        photoUrl: String,
        dailyProg: String,
        dietary: String,
        activitiesList: String
    ) {
        val updated = CampSettings(
            photoAlbumUrl = photoUrl.trim(),
            dailyProgram = dailyProg.trim(),
            dietaryMenu = dietary.trim(),
            activities = activitiesList.trim()
        )
        db.collection("config").document("camp_settings").set(updated.toMap())
            .addOnSuccessListener {
                _statusMessage.value = "تم تحديث البيانات بنجاح"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "فشل التحديث: ${e.localizedMessage}"
            }
    }

    // Push new custom notification to Firestore
    fun sendNotification(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) {
            _statusMessage.value = "يرجى تعبئة عنوان ونص الإشعار"
            return
        }

        val ref = db.collection("notifications").document()
        val notification = CampNotification(
            id = ref.id,
            title = title.trim(),
            body = body.trim(),
            createdAt = System.currentTimeMillis()
        )

        ref.set(notification.toMap())
            .addOnSuccessListener {
                _statusMessage.value = "تم إرسال الإشعار لجميع الأولياء بنجاح"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "فشل في إرسال الإشعار: ${e.localizedMessage}"
            }
    }

    // Create a new Facebook-style album post
    fun createAlbumPost(albumName: String, albumUrl: String) {
        if (albumName.isBlank() || albumUrl.isBlank()) {
            _statusMessage.value = "يرجى ملء جميع حقول الألبوم"
            return
        }

        _isLoading.value = true
        val ref = db.collection("album_posts").document()
        val post = CampAlbumPost(
            id = ref.id,
            albumName = albumName.trim(),
            albumUrl = albumUrl.trim(),
            createdAt = System.currentTimeMillis()
        )

        ref.set(post.toMap())
            .addOnSuccessListener {
                _statusMessage.value = "تم نشر ألبوم الصور بنجاح 🎉"
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "فشل النشر: ${e.localizedMessage}"
                _isLoading.value = false
            }
    }

    // Delete an album post
    fun deleteAlbumPost(postId: String) {
        db.collection("album_posts").document(postId).delete()
            .addOnSuccessListener {
                _statusMessage.value = "تم حذف الألبوم بنجاح"
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "خطأ في حذف الألبوم: ${e.localizedMessage}"
            }
    }

    // Add comment to an album post
    fun addCommentToAlbum(postId: String, commentText: String, isPrivate: Boolean) {
        if (commentText.isBlank()) return

        // Author is Admin or the current approved parent
        val authorName = if (_isAdmin.value) "إدارة المخيم (Admin) 👑" else (_currentUser.value?.parentName ?: "ولي أمر")
        val child = if (_isAdmin.value) "" else (_currentUser.value?.childName ?: "")

        val newComment = AlbumComment(
            id = System.currentTimeMillis().toString() + "_" + (100..999).random(),
            parentName = authorName,
            childName = child,
            text = commentText.trim(),
            isPrivate = isPrivate,
            createdAt = System.currentTimeMillis()
        )

        val docRef = db.collection("album_posts").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val commentsRaw = snapshot.get("comments") as? List<Map<String, Any>> ?: emptyList()
            val updatedComments = commentsRaw.toMutableList()
            updatedComments.add(newComment.toMap())
            transaction.update(docRef, "comments", updatedComments)
        }.addOnSuccessListener {
            _statusMessage.value = "تمت إضافة تعليقك"
        }.addOnFailureListener { e ->
            _statusMessage.value = "فشل إضافة التعليق: ${e.localizedMessage}"
        }
    }
}
