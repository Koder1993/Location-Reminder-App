package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.wrapEspressoIdlingResource
import kotlinx.coroutines.withContext

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private val reminderDTOList = mutableListOf<ReminderDTO>()
    private var isError = false

    fun setError(isError: Boolean) {
        this.isError = isError
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        if (isError) {
            Result.Error("Error while fetching reminders list")
        } else {
            Result.Success(reminderDTOList)
        }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderDTOList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminderDTO = reminderDTOList.firstOrNull { it.id == id }
        return reminderDTO?.let {
            Result.Success(it)
        } ?: run {
            Result.Error("Reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminderDTOList.clear()
    }

    override suspend fun deleteReminder(reminderId: String) {
        val reminderDTO = reminderDTOList.firstOrNull { it.id == reminderId }
        reminderDTO?.let {
            reminderDTOList.remove(it)
        }
    }

//    /**
//     * Get the reminders list from the local db
//     * @return Result the holds a Success with all the reminders or an Error object with the error message
//     */
//    override suspend fun getReminders(): Result<List<ReminderDTO>> = wrapEspressoIdlingResource {
//        withContext(ioDispatcher) {
//            return@withContext try {
//                Result.Success(remindersDao.getReminders())
//            } catch (ex: Exception) {
//                Result.Error(ex.localizedMessage)
//            }
//        }
//    }
//
//    /**
//     * Insert a reminder in the db.
//     * @param reminder the reminder to be inserted
//     */
//    override suspend fun saveReminder(reminder: ReminderDTO) = wrapEspressoIdlingResource {
//        withContext(ioDispatcher) {
//            remindersDao.saveReminder(reminder)
//        }
//    }
//
//    /**
//     * Get a reminder by its id
//     * @param id to be used to get the reminder
//     * @return Result the holds a Success object with the Reminder or an Error object with the error message
//     */
//    override suspend fun getReminder(id: String): Result<ReminderDTO> = wrapEspressoIdlingResource {
//        withContext(ioDispatcher) {
//            try {
//                val reminder = remindersDao.getReminderById(id)
//                if (reminder != null) {
//                    return@withContext Result.Success(reminder)
//                } else {
//                    return@withContext Result.Error("Reminder not found!")
//                }
//            } catch (e: Exception) {
//                return@withContext Result.Error(e.localizedMessage)
//            }
//        }
//    }
//
//    override suspend fun deleteReminder(reminderId: String) {
//        wrapEspressoIdlingResource {
//            withContext(ioDispatcher) {
//                remindersDao.deleteReminder(reminderId)
//            }
//        }
//    }
//
//    /**
//     * Deletes all the reminders in the db
//     */
//    override suspend fun deleteAllReminders() {
//        wrapEspressoIdlingResource {
//            withContext(ioDispatcher) {
//                remindersDao.deleteAllReminders()
//            }
//        }
//    }
}