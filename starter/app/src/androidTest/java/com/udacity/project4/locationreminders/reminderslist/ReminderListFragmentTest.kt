package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var dataSource: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val reminderItem1 =
        ReminderDTO("ReminderTitle1", "Description1", "Location1", 25.0, 38.0, "1")
    private val reminderItem2 =
        ReminderDTO("ReminderTitle2", "Description2", "Location2", 33.0, 45.0, "2")

    @Before
    fun setup() {
        stopKoin()
        appContext = getApplicationContext()
        val module = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        startKoin {
            modules(listOf(module))
        }
        dataSource = get()

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun test_clickFabAndNavigateToReminderFragment() {
        val fragmentScenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        val navController = mock(NavController::class.java)
        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun test_reminderListAndNoReminders() {
        val fragmentScenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)

        val navController = mock(NavController::class.java)
        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withText(R.string.no_data)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withText(reminderItem1.title)).check(doesNotExist())
    }

    @Test
    fun test_reminderListAndDisplayedInUi() {
        runBlocking {
            dataSource.saveReminder(reminderItem1)
            dataSource.saveReminder(reminderItem2)
        }

        val fragmentScenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragmentScenario)
        val navController = mock(NavController::class.java)
        fragmentScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withText(reminderItem1.title)).check(matches(isDisplayed()))
        onView(withText(reminderItem2.title)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
    }
}