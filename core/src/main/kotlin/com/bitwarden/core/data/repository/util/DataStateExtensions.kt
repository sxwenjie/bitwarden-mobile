package com.bitwarden.core.data.repository.util

import com.bitwarden.core.annotation.OmitFromCoverage
import com.bitwarden.core.data.repository.model.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update

/**
 * Maps the data inside a [DataState] with the given [transform].
 */
inline fun <T : Any?, R : Any?> DataState<T>.map(
    transform: (T) -> R,
): DataState<R> = when (this) {
    is DataState.Loaded -> DataState.Loaded(transform(data))
    is DataState.Loading -> DataState.Loading
    is DataState.Pending -> DataState.Pending(transform(data))
    is DataState.Error -> DataState.Error(error, data?.let(transform))
    is DataState.NoNetwork -> DataState.NoNetwork(data?.let(transform))
}

/**
 * Maps the data inside a [DataState] with the given [transform] regardless of the data's
 * nullability.
 */
inline fun <T : Any?, R : Any?> DataState<T>.mapNullable(
    transform: (T?) -> R,
): DataState<R> = when (this) {
    is DataState.Loaded -> DataState.Loaded(data = transform(data))
    is DataState.Loading -> DataState.Loading
    is DataState.Pending -> DataState.Pending(data = transform(data))
    is DataState.Error -> DataState.Error(error = error, data = transform(data))
    is DataState.NoNetwork -> DataState.NoNetwork(data = transform(data))
}

/**
 * Emits all values of a [DataState] [Flow] until it emits a [DataState.Loaded].
 */
fun <T : Any?> Flow<DataState<T>>.takeUntilLoaded(): Flow<DataState<T>> = transformWhile {
    emit(it)
    it !is DataState.Loaded
}

/**
 * Updates the [DataState] to [DataState.Pending] if there is data present or [DataState.Loading]
 * if no data is present.
 */
fun <T : Any?> MutableStateFlow<DataState<T>>.updateToPendingOrLoading() {
    update { dataState ->
        dataState.data
            ?.let { data -> DataState.Pending(data = data) }
            ?: DataState.Loading
    }
}

/**
 * Combines the [dataState1] and [dataState2] [DataState]s together using the provided [transform].
 *
 * This function will internally manage the final `DataState` type that is returned. This is done
 * by prioritizing each if the states in this order:
 *
 * - [DataState.Error]
 * - [DataState.NoNetwork]
 * - [DataState.Loading]
 * - [DataState.Pending]
 * - [DataState.Loaded]
 *
 * This priority order ensures that the total state is accurately reflecting the underlying states.
 * If one of the `DataState`s has a higher priority than the other, the output will be the highest
 * priority. For example, if one `DataState` is `DataState.Loaded` and another is `DataState.Error`
 * then the returned `DataState` will be `DataState.Error`.
 *
 * The payload of the final `DataState` be created by the `transform` lambda which will be invoked
 * whenever the payloads of both `dataState1` and `dataState2` are not null. In the scenario where
 * one or both payloads are null, the resulting `DataState` will have a null payload.
 */
fun <T1, T2, R> combineDataStates(
    dataState1: DataState<T1>,
    dataState2: DataState<T2>,
    transform: (t1: T1, t2: T2) -> R,
): DataState<R> {
    // Wraps the `transform` lambda to allow null data to be passed in. If either of the passed in
    // values are null, the regular transform will not be invoked and null is returned.
    val nullableTransform: (T1?, T2?) -> R? = { t1, t2 ->
        if (t1 != null && t2 != null) transform(t1, t2) else null
    }
    return when {
        // Error states have highest priority, fail fast.
        dataState1 is DataState.Error -> {
            DataState.Error(
                error = dataState1.error,
                data = nullableTransform(dataState1.data, dataState2.data),
            )
        }

        dataState2 is DataState.Error -> {
            DataState.Error(
                error = dataState2.error,
                data = nullableTransform(dataState1.data, dataState2.data),
            )
        }

        dataState1 is DataState.NoNetwork || dataState2 is DataState.NoNetwork -> {
            DataState.NoNetwork(nullableTransform(dataState1.data, dataState2.data))
        }

        // Something is still loading, we will wait for all the data.
        dataState1 is DataState.Loading || dataState2 is DataState.Loading -> DataState.Loading

        // Pending state for everything while any one piece of data is updating.
        dataState1 is DataState.Pending || dataState2 is DataState.Pending -> {
            @Suppress("UNCHECKED_CAST")
            DataState.Pending(transform(dataState1.data as T1, dataState2.data as T2))
        }

        // Both states are Loaded and have data
        else -> {
            @Suppress("UNCHECKED_CAST")
            DataState.Loaded(transform(dataState1.data as T1, dataState2.data as T2))
        }
    }
}

/**
 * Combines the [dataState1], [dataState2], and [dataState3] [DataState]s together using the
 * provided [transform].
 *
 * See [combineDataStates] for details.
 */
@OmitFromCoverage
fun <T1, T2, T3, R> combineDataStates(
    dataState1: DataState<T1>,
    dataState2: DataState<T2>,
    dataState3: DataState<T3>,
    transform: (t1: T1, t2: T2, t3: T3) -> R,
): DataState<R> =
    dataState1
        .combineDataStatesWith(dataState2) { t1, t2 -> t1 to t2 }
        .combineDataStatesWith(dataState3) { t1t2Pair, t3 ->
            transform(t1t2Pair.first, t1t2Pair.second, t3)
        }

/**
 * Combines the [dataState1], [dataState2], [dataState3], and [dataState4] [DataState]s together
 * using the provided [transform].
 *
 * See [combineDataStates] for details.
 */
@OmitFromCoverage
fun <T1, T2, T3, T4, R> combineDataStates(
    dataState1: DataState<T1>,
    dataState2: DataState<T2>,
    dataState3: DataState<T3>,
    dataState4: DataState<T4>,
    transform: (t1: T1, t2: T2, t3: T3, t4: T4) -> R,
): DataState<R> =
    dataState1
        .combineDataStatesWith(dataState2) { t1, t2 -> t1 to t2 }
        .combineDataStatesWith(dataState3) { t1t2Pair, t3 ->
            Triple(t1t2Pair.first, t1t2Pair.second, t3)
        }
        .combineDataStatesWith(dataState4) { t1t2t3Triple, t3 ->
            transform(t1t2t3Triple.first, t1t2t3Triple.second, t1t2t3Triple.third, t3)
        }

/**
 * Combines [dataState2] with the given [DataState] using the provided [transform].
 *
 * See [combineDataStates] for details.
 */
@OmitFromCoverage
fun <T1, T2, R> DataState<T1>.combineDataStatesWith(
    dataState2: DataState<T2>,
    transform: (t1: T1, t2: T2) -> R,
): DataState<R> =
    combineDataStates(this, dataState2, transform)
