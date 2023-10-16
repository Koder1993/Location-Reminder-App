package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.util.MainDispatcherRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var repository: FakeDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val reminderDataItem =
        ReminderDataItem("ReminderTitle1", "Description1", "Location1", 25.0, 38.0, "1")
    private val reminderDataItemNoTitle =
        ReminderDataItem("", "Description2", "Location2", 33.0, 45.0, "2")
    private val reminderDataItemNoLocation =
        ReminderDataItem("ReminderTitle3", "Description2", null, 0.0, 0.0, "3")


    @Before
    fun setup() {
        repository = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), repository)
    }

    @After
    fun cleanUp() {
        stopKoin()
    }

    @Test
    fun test_saveReminderAndAddsReminderToDataSource() = runTest {
        viewModel.validateAndSaveReminder(reminderDataItem) // saves data to repository
        val reminderResult = repository.getReminder("1") as Result.Success

        assertThat(reminderResult.data.title, `is`(reminderDataItem.title))
        assertThat(reminderResult.data.description, `is`(reminderDataItem.description))
        assertThat(reminderResult.data.location, `is`(reminderDataItem.location))
        assertThat(reminderResult.data.latitude, `is`(reminderDataItem.latitude))
        assertThat(reminderResult.data.longitude, `is`(reminderDataItem.longitude))
        assertThat(reminderResult.data.id, `is`(reminderDataItem.id))
    }

    // testing reminder not found in repository error scenario
    @Test
    fun test_reminderNotFoundInRepository() = runTest {
        repository.deleteAllReminders()
        viewModel.validateAndSaveReminder(reminderDataItem) // saves data to repository
        val reminderResult = repository.getReminder("2") as Result.Error

        assertThat(reminderResult.message, `is`("Error while fetching reminder"))
    }

    // testing reminder error  in repository using flag to mock exception scenario
    @Test
    fun test_reminderErrorUsingFlag() = runTest {
        repository.deleteAllReminders()
        viewModel.validateAndSaveReminder(reminderDataItem) // saves data to repository
        repository.setError(true)
        val reminderResult = repository.getReminder("1") as Result.Error

        assertThat(reminderResult.message, `is`("Error while fetching reminder"))
    }


    @Test
    fun test_saveReminderAndCheckLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertThat(viewModel.showLoading.value, `is`(true))
        advanceUntilIdle()
        assertThat(viewModel.showLoading.value, `is`(false))
    }

    @Test
    fun test_validateData_missingTitle_showSnackAndReturnFalse() {
        val isEnteredDataValid = viewModel.validateEnteredData(reminderDataItemNoTitle)

        assertThat(isEnteredDataValid, `is`(false))
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
    }

    @Test
    fun test_validateData_missingLocation_showSnackAndReturnFalse() {
        val isEnteredDataValid = viewModel.validateEnteredData(reminderDataItemNoLocation)

        assertThat(isEnteredDataValid, `is`(false))
        assertThat(viewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
    }

    @Test
    fun test_editReminderSetsLiveDataOfReminderToBeEdited() = runTest {
        repository.deleteAllReminders()
        viewModel.validateAndSaveReminder(reminderDataItemNoTitle)
        viewModel.updateReminder(reminderDataItem)

        assertThat(viewModel.reminderTitle.value, `is`(reminderDataItem.title))
        assertThat(viewModel.reminderDescription.value, `is`(reminderDataItem.description))
        assertThat(viewModel.reminderSelectedLocationStr.value, `is`(reminderDataItem.location))
        assertThat(viewModel.latitude.value, `is`(reminderDataItem.latitude))
        assertThat(viewModel.longitude.value, `is`(reminderDataItem.longitude))
    }

    @Test
    fun test_onClearsReminderLiveData() = runTest {
        repository.deleteAllReminders()
        viewModel.validateAndSaveReminder(reminderDataItemNoTitle)
        viewModel.updateReminder(reminderDataItem)

        viewModel.onClear()

        assertThat(viewModel.reminderTitle.value, `is`(nullValue()))
        assertThat(viewModel.reminderDescription.value, `is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.value, `is`(nullValue()))
        assertThat(viewModel.latitude.value, `is`(nullValue()))
        assertThat(viewModel.longitude.value, `is`(nullValue()))
    }
}