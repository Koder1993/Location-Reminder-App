package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private val reminderItem1 =
        ReminderDTO("ReminderTitle1", "Description1", "Location1", 25.0, 38.0, "1")
    private val reminderItem2 =
        ReminderDTO("ReminderTitle2", "Description2", "Location2", 33.0, 45.0, "2")
    private val reminderItem3 =
        ReminderDTO("ReminderTitle3", "Description3", "Location3", 82.0, 117.0, "3")

    private lateinit var repository: FakeDataSource
    private lateinit var viewModel: RemindersListViewModel
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        repository = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), repository)
    }

    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun test_loadRemindersLoadsThreeReminders() = runTest {
        repository.deleteAllReminders()
        repository.saveReminder(reminderItem1)
        repository.saveReminder(reminderItem2)
        repository.saveReminder(reminderItem3)

        viewModel.loadReminders()
        assertThat(viewModel.remindersList.value?.size, `is` (3))
    }

    @Test
    fun test_invalidateShowNoDataShowNoDataIsTrue() = runTest {
        repository.deleteAllReminders()
        viewModel.loadReminders()

        assertThat(viewModel.remindersList.value?.isEmpty(), `is` (true))
        assertThat(viewModel.showNoData.value, `is` (true))
    }

    @Test
    fun test_loadRemindersShouldReturnError() = runTest {
        repository.setError(true)
        viewModel.loadReminders()
        assertThat(viewModel.showSnackBar.value, `is`("Error while fetching reminders list"))
    }

    @Test
    fun test_loadRemindersCheckLoading() = runTest {

        Dispatchers.setMain(StandardTestDispatcher())

        repository.deleteAllReminders()
        repository.saveReminder(reminderItem1)
        repository.saveReminder(reminderItem2)
        viewModel.loadReminders()

        assertThat(viewModel.showLoading.value, `is`(true))

        advanceUntilIdle()

        assertThat(viewModel.showLoading.value, `is`(false))
    }

}