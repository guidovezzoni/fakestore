package com.guidovezzoni.fakestore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.data.repository.ProductRepositoryImpl
import com.guidovezzoni.fakestore.data.repository.UserRepositoryImpl
import com.guidovezzoni.fakestore.domain.repository.ProductRepository
import com.guidovezzoni.fakestore.domain.repository.UserRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltDependencyGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun GIVEN_the_Hilt_graph_is_assembled_WHEN_ProductRepository_is_injected_THEN_it_is_a_non_null_ProductRepositoryImpl() {
        assertNotNull(productRepository)
        assertTrue(productRepository is ProductRepositoryImpl)
    }

    @Test
    fun GIVEN_the_Hilt_graph_is_assembled_WHEN_UserRepository_is_injected_THEN_it_is_a_non_null_UserRepositoryImpl() {
        assertNotNull(userRepository)
        assertTrue(userRepository is UserRepositoryImpl)
    }
}
