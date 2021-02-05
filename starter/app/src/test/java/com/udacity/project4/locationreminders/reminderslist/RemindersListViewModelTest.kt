package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var reminderListViewModel: RemindersListViewModel
    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setupViewModel(){
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 100.0, 100.0, "id1")
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 100.0, 100.0, "id2")
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3", 100.0, 100.0, "id3")
        val reminder4 = ReminderDTO("Title4", "Description4", "Location4", 100.0, 100.0, "id4")
        reminderDataSource = FakeDataSource()
        reminderDataSource.addReminders(reminder1, reminder2, reminder3, reminder4)
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @After
    fun stop(){
        stopKoin()
    }

    @Test
    fun loadReminder_loading(){
        mainCoroutineRule.pauseDispatcher()

        reminderListViewModel.loadReminders()

        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminder_returnSuccess(){
        reminderListViewModel.loadReminders()
        val reminderList = reminderListViewModel.remindersList.getOrAwaitValue()

        assertThat(reminderList[0].id, `is`("id1"))
        assertThat(reminderList[1].id, `is`("id2"))
        assertThat(reminderList[3].id, `is`("id4"))
        assertThat(reminderList.size, `is`(4))
    }

    @Test
    fun loadReminder_showNotFound(){
        reminderDataSource.setReturnError(true)
        reminderListViewModel.loadReminders()

        assertThat(reminderListViewModel.showSnackBar.getOrAwaitValue(), `is`("Reminders not found"))
        assertThat(reminderListViewModel.showNoData.getOrAwaitValue() , `is`(true))
    }

}