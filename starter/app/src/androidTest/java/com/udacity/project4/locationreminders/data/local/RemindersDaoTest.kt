package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private val reminderItem1 =
        ReminderDTO("ReminderTitle1", "Description1", "Location1", 25.0, 38.0, "1")
    private val reminderItem2 =
        ReminderDTO("ReminderTitle2", "Description2", "Location2", 33.0, 45.0, "2")

    @Before
    fun setup() {
        // using an in-memory database because the information stored here disappears when the process is destroy
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun test_insertAll() = runBlocking {
        database.reminderDao().saveReminder(reminderItem1)
        database.reminderDao().saveReminder(reminderItem2)
        assertThat(database.reminderDao().getReminders().size, `is`(2))
    }

    @Test
    fun test_reminderGet_reminderPresent() = runBlocking {
        database.reminderDao().saveReminder(reminderItem1)
        database.reminderDao().saveReminder(reminderItem2)
        val reminder = database.reminderDao().getReminderById(reminderItem1.id)
        assertThat(reminder as ReminderDTO, notNullValue())
        assertThat(reminder.title, `is`(reminderItem1.title))
        assertThat(reminder.description, `is`(reminderItem1.description))
        assertThat(reminder.location, `is`(reminderItem1.location))
        assertThat(reminder.latitude, `is`(reminderItem1.latitude))
        assertThat(reminder.longitude, `is`(reminderItem1.longitude))
    }

    @Test
    fun test_reminderGet_reminderNotFound() = runBlocking {
        database.reminderDao().saveReminder(reminderItem1)
        val reminder = database.reminderDao().getReminderById("4")
        assertThat(reminder, nullValue())
    }

    @Test
    fun test_insertAllAndDeleteAll() = runBlocking {
        database.reminderDao().saveReminder(reminderItem1)
        database.reminderDao().saveReminder(reminderItem2)
        assertThat(database.reminderDao().getReminders().size, `is`(2))
        database.reminderDao().deleteAllReminders()
        assertThat(database.reminderDao().getReminders().isEmpty(), `is`(true))
    }

    @Test
    fun test_insertRemindersAndDeleteReminderById() = runBlocking {
        database.reminderDao().saveReminder(reminderItem1)
        database.reminderDao().saveReminder(reminderItem2)
        database.reminderDao().deleteReminder(reminderItem1.id)
        val reminder = database.reminderDao().getReminderById(reminderItem1.id)
        assertThat(reminder, nullValue())
    }

}