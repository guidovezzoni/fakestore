package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository

class ToggleFavouriteUseCase(private val repository: FavouritesRepository) {
    suspend operator fun invoke(productId: Int, shouldBeFavourite: Boolean): Result<Unit> =
        runCatching {
            if (shouldBeFavourite) {
                repository.addFavourite(productId)
            } else {
                repository.removeFavourite(productId)
            }
        }
}
