package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private val reminderItem1 =
        ReminderDTO("ReminderTitle1", "Description1", "Location1", 25.0, 38.0, "1")
    private val reminderItem2 =
        ReminderDTO("ReminderTitle2", "Description2", "Location2", 33.0, 45.0, "2")

    private lateinit var database: RemindersDatabase
    private lateinit var remindersRepository: RemindersLocalRepository

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // testing with an in-memory database because it won't survive stopping the process
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        remindersRepository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun test_saveRemindersAndDeletesAllReminders() = runBlocking {
        remindersRepository.saveReminder(reminderItem1)
        remindersRepository.saveReminder(reminderItem2)
        remindersRepository.deleteAllReminders()

        val result = remindersRepository.getReminders() as Result.Success
        assertThat(result.data, notNullValue())
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun test_saveReminderAndRetrievesReminderById() = runBlocking {
        remindersRepository.saveReminder(reminderItem1)
        val result = remindersRepository.getReminder(reminderItem1.id) as Result.Success
        assertThat(result.data, notNullValue())
        assertThat(result.data.id, `is`(reminderItem1.id))
        assertThat(result.data.title, `is`(reminderItem1.title))
        assertThat(result.data.location, `is`(reminderItem1.location))
        assertThat(result.data.latitude, `is`(reminderItem1.latitude))
        assertThat(result.data.longitude, `is`(reminderItem1.longitude))
        assertThat(result.data.description, `is`(reminderItem1.description))
    }

    @Test
    fun test_saveRemindersAndRetrievesAllReminders() = runBlocking {
        remindersRepository.saveReminder(reminderItem1)
        remindersRepository.saveReminder(reminderItem2)
        val result = remindersRepository.getReminders() as Result.Success
        assertThat(result.data, notNullValue())
        assertThat(result.data.size, `is`(2))
        assertThat(result.data[0].title, `is`(reminderItem1.title))
        assertThat(result.data[1].title, `is`(reminderItem2.title))
    }

    @Test
    fun test_saveRemindersAndDeletesOneReminderById() = runBlocking {
        remindersRepository.saveReminder(reminderItem1)
        remindersRepository.saveReminder(reminderItem2)
        remindersRepository.deleteReminder(reminderItem1.id)

        val result = remindersRepository.getReminders() as Result.Success
        assertThat(result.data, notNullValue())
        assertThat(result.data.size, `is`(1))
        assertThat(result.data[0].title, `is`(reminderItem2.title))
    }

    @Test
    fun test_getReminderError() = runBlocking {
        remindersRepository.saveReminder(reminderItem1)
        remindersRepository.saveReminder(reminderItem2)

        val result = remindersRepository.getReminder("3") as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
}