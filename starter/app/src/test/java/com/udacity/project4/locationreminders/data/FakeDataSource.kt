package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

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
}