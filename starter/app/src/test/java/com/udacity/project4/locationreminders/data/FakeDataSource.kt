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

    // error case can be faked manually by setting isError flag OR using wrong id
    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (isError) {
            return Result.Error("Error while fetching reminder")
        }
        val reminderDTO = reminderDTOList.firstOrNull { it.id == id }
        return reminderDTO?.let {
            Result.Success(it)
        } ?: run {
            Result.Error("Reminder not found!")
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