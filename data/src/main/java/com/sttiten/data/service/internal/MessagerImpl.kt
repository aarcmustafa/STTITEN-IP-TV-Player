package com.sttiten.iptv.data.service.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MessagerImpl @Inject constructor() : Messager {
    private val _message: MutableStateFlow<Message> = MutableStateFlow(Message.Dynamic.EMPTY)
    override val message: StateFlow<Message> get() = _message.asStateFlow()

    private var job: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun emit(message: Message) {
        job?.cancel()
        job = coroutineScope.launch {
            _message.value = message
            delay(message.duration)
            _message.value = Message.Dynamic.EMPTY
        }
    }

    override fun emit(message: String) {
        emit(Message.Dynamic(message, Message.LEVEL_WARN, "", Message.TYPE_SNACK))
    }
}