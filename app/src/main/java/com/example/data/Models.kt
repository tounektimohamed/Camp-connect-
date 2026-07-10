package com.example.data

import java.util.Date

data class CampUser(
    val id: String = "",
    val parentName: String = "",
    val childName: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val registeredAt: Long = System.currentTimeMillis()
) {
    // Firestore-friendly map converter
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "parentName" to parentName,
            "childName" to childName,
            "status" to status,
            "registeredAt" to registeredAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): CampUser {
            return CampUser(
                id = map["id"] as? String ?: "",
                parentName = map["parentName"] as? String ?: "",
                childName = map["childName"] as? String ?: "",
                status = map["status"] as? String ?: "pending",
                registeredAt = (map["registeredAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

data class CampSettings(
    val photoAlbumUrl: String = "https://photos.google.com",
    val dailyProgram: String = "البرنامج اليومي الافتراضي:\n08:00 - التجمع والاستقبال\n09:00 - تمارين صباحية\n10:00 - نشاط رياضي خارجي\n12:00 - وجبة الغداء\n14:00 - ورشات علمية وفنية\n16:00 - العودة للمنزل",
    val dietaryMenu: String = "البرنامج الغذائي الأسبوعي:\nالأحد: أرز بالدجاج + سلطة + فواكه\nالأثنين: معكرونة + حساء + عصير طبيعي\nالثلاثاء: كسكسي بالخضار + ياغورت\nالأربعاء: لحم مشوي + سلطة مشوية\nالخميس: سمك + بطاطا مقلية + تحلية",
    val activities: String = "أهم الأنشطة المقررة:\n- مسابقة الابتكار والعلوم\n- رحلة استكشافية للغابة المجاورة\n- بطولة كرة القدم الشاطئية\n- سهرات ثقافية وعروض مسرحية"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "photoAlbumUrl" to photoAlbumUrl,
            "dailyProgram" to dailyProgram,
            "dietaryMenu" to dietaryMenu,
            "activities" to activities
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): CampSettings {
            return CampSettings(
                photoAlbumUrl = map["photoAlbumUrl"] as? String ?: "https://photos.google.com",
                dailyProgram = map["dailyProgram"] as? String ?: "",
                dietaryMenu = map["dietaryMenu"] as? String ?: "",
                activities = map["activities"] as? String ?: ""
            )
        }
    }
}

data class CampNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "body" to body,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): CampNotification {
            return CampNotification(
                id = id,
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

data class AlbumComment(
    val id: String = "",
    val parentName: String = "",
    val childName: String = "",
    val text: String = "",
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "parentName" to parentName,
            "childName" to childName,
            "text" to text,
            "isPrivate" to isPrivate,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): AlbumComment {
            return AlbumComment(
                id = map["id"] as? String ?: "",
                parentName = map["parentName"] as? String ?: "",
                childName = map["childName"] as? String ?: "",
                text = map["text"] as? String ?: "",
                isPrivate = map["isPrivate"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

data class CampAlbumPost(
    val id: String = "",
    val albumName: String = "",
    val albumUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val comments: List<AlbumComment> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "albumName" to albumName,
            "albumUrl" to albumUrl,
            "createdAt" to createdAt,
            "comments" to comments.map { it.toMap() }
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any>): CampAlbumPost {
            val commentsRaw = map["comments"] as? List<Map<String, Any>> ?: emptyList()
            val commentsList = commentsRaw.map { AlbumComment.fromMap(it) }
            return CampAlbumPost(
                id = id,
                albumName = map["albumName"] as? String ?: "",
                albumUrl = map["albumUrl"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                comments = commentsList
            )
        }
    }
}
