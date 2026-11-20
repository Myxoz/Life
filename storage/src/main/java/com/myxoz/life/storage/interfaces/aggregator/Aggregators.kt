package com.myxoz.life.storage.interfaces.aggregator

class Aggregators(
    val peopleEventAggregator: PeopleEventAggregator,
    val lifeRescheduling: LifeReschedulingAggregator,
    val alarmAggregators: AlarmAggregator
)