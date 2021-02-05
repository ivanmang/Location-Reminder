package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setupViewModel(){
        reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }
    @After
    fun stop(){
        stopKoin()
    }

    @Test
    fun saveReminder_returnSuccess() = runBlockingTest {
        val reminder = ReminderDataItem("Title1", "Description1", "Location1", 100.0, 100.0, "id1")
        saveReminderViewModel.saveReminder(reminder)
        val result = reminderDataSource.getReminder(reminder.id) as Result.Success
        val reminderStored = result.data
        assertThat(reminderStored.id, `is`(reminder.id))
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }


    @Test
    fun saveReminder_loading() {
        val reminder = ReminderDataItem("Title1", "Description1", "Location1", 100.0, 100.0, "id1")
        mainCoroutineRule.pauseDispatcher()

        saveReminderViewModel.saveReminder(reminder)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))

    }

    @Test
    fun validateEnteredData_returnSuccess() {
        val reminder = ReminderDataItem("Title1", "Description1", "Location1", 100.0, 100.0, "id1")
        assertThat(saveReminderViewModel.validateEnteredData(reminder), `is`(true))
    }

    @Test
    fun validateEnteredData_returnError() {
        val reminder1 = ReminderDataItem(null, "Description1", "Location1", 100.0, 100.0, "id1")
        val reminder2 = ReminderDataItem("Tilte", "Description1", null, 100.0, 100.0, "id1")
        assertThat(saveReminderViewModel.validateEnteredData(reminder1), `is`(false))
        assertThat(saveReminderViewModel.validateEnteredData(reminder2), `is`(false))
    }


}