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

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (isError) {
            return Result.Error("Error while fetching reminders list")
        }
        return try {
            Result.Success(reminderDTOList)
        } catch (ex: Exception) {
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderDTOList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return try {
            val reminderDTO = reminderDTOList.firstOrNull { it.id == id }
            reminderDTO?.let {
                Result.Success(it)
            } ?: run {
                Result.Error("Reminder not found")
            }
        } catch(e: Exception) {
            Result.Error(e.localizedMessage)
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
}