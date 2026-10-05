package com.creativeblisslabs.peqlearn.app.ui.screens

object CircleOfFifths {
    private val circleOrder = listOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)

    fun getCircleNames(names: List<String>): List<String> {
        return circleOrder.map { names[it] }
    }

    fun getRootFromCircleIndex(index: Int): Int {
        return circleOrder[index]
    }

    fun getCircleIndexFromRoot(rootIdx: Int): Int {
        return circleOrder.indexOf(rootIdx)
    }
}
