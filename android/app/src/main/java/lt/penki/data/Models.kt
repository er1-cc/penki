package lt.penki.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val email: String = "",
    val full_name: String = "",
    val role: String = "pending",
    val active: Boolean = false,
    val created_at: String? = null
) {
    val isAdmin: Boolean get() = role == "admin" && active
    val isTeacher: Boolean get() = active && (role == "teacher" || role == "admin")
}

@Serializable
data class Klass(
    val id: String = "",
    val name: String = "",
    val active: Boolean = true
)

@Serializable
data class Invite(
    val id: String = "",
    val email: String = "",
    val code: String = "",
    val created_at: String? = null,
    val expires_at: String? = null,
    val redeemed_at: String? = null
)

@Serializable
data class Rating(
    val id: String? = null,
    val teacher_id: String = "",
    val class_id: String = "",
    val lesson_no: Int = 1,
    val score: Int = 0,
    val comment: String = "",
    val rated_on: String? = null,
    val created_at: String? = null,
    val classes: Klass? = null,
    val profiles: Profile? = null
) {
    val className: String get() = classes?.name ?: "Klasė"
    val teacherName: String get() = profiles?.full_name?.takeIf { it.isNotBlank() } ?: "Mokytojas"
}

@Serializable
data class AuthUser(
    val id: String = "",
    val email: String? = null
)

@Serializable
data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val user: AuthUser? = null,
    val id: String? = null,
    val email: String? = null
)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class SignUpRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refresh_token: String)

@Serializable
data class NewClass(val name: String)

@Serializable
data class PatchProfile(val full_name: String)

@Serializable
data class PatchActive(val active: Boolean)

@Serializable
data class NewInvite(val email: String, val code: String, val created_by: String)

@Serializable
data class NewRating(
    val teacher_id: String,
    val class_id: String,
    val lesson_no: Int,
    val score: Int,
    val comment: String,
    val rated_on: String
)

/** Mokytojo ir klasės priskyrimas (adminas nurodo, kurias klases mokytojas vertina). */
@Serializable
data class TeacherClass(
    val teacher_id: String = "",
    val class_id: String = ""
)

@Serializable
data class NewTeacherClass(
    val teacher_id: String,
    val class_id: String
)

/** Pamokos pabaigos laikas, saugomas serveryje (HH:mm). */
@Serializable
data class LessonTime(
    val lesson_no: Int = 1,
    val ends_at: String = "",
    val teacher_id: String = ""
) {
    val hhmm: String get() = ends_at.take(5)
}

@Serializable
data class NewLessonTime(
    val teacher_id: String,
    val lesson_no: Int,
    val ends_at: String
)

@Serializable
data class GoogleSheetsConfig(
    val id: Boolean = true,
    val enabled: Boolean = false,
    val spreadsheet_id: String = "",
    val sheet_name: String = "Vertinimai",
    val sheet_per_teacher: Boolean = false
)

@Serializable
data class RedeemResult(val ok: Boolean = false, val error: String? = null)

@Serializable
data class RedeemRequest(val p_code: String)

@Serializable
data class NameRequest(val p_name: String)

@Serializable
data class Feedback(
    val id: String = "",
    val teacher_id: String = "",
    val message: String = "",
    val read: Boolean = false,
    val created_at: String? = null,
    val profiles: Profile? = null
) {
    val teacherName: String get() = profiles?.full_name?.takeIf { it.isNotBlank() } ?: profiles?.email ?: "Mokytojas"
}

@Serializable
data class NewFeedback(
    val teacher_id: String,
    val message: String
)

@Serializable
data class PatchFeedbackRead(
    val read: Boolean = true
)

@Serializable
data class SheetsAppendRequest(val rating_id: String)

@Serializable
data class SheetsAppendResult(
    val ok: Boolean = false,
    val exported: Boolean = false,
    val skipped: Boolean = false,
    val error: String? = null
)
