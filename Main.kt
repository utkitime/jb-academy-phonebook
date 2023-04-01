package phonebook

import java.io.File
import kotlin.math.sqrt

fun main() {

    val separator = File.separator
    val path = "${separator}Users${separator}utkitime${separator}"
    val phoneBookPath = "${path}directory.txt"
    val searchItemsPath = "${path}find.txt"
    val phoneBook = PhoneBook(phoneBookPath, searchItemsPath)
    phoneBook.linearSearchFirstRun()
    phoneBook.startSortThenSearch("bubble", "jump")
    phoneBook.startSortThenSearch("quick", "binary")
    phoneBook.startHashTableSearch()
}

class PhoneBook(phoneBookPath: String, searchItemsPath: String) {
    private val phoneBook = File(phoneBookPath).readLines().toTypedArray()
    private val searchItems = File(searchItemsPath).readLines()
    private val sortToSearchRatio = 10
    private var maxFunctionExecutionTime: Long = Long.MAX_VALUE

    enum class SortStatus {
        TOO_LONG_EXECUTION,
        FINISHED
    }

    private val sortMethod = mapOf("bubble" to ::sortListBubble, "quick" to ::startQuickSort)
    private val searchMethod =
        mapOf("linear" to ::startLinearSearch, "jump" to ::startJumpSearch, "binary" to ::startBinarySearch)

    fun startSortThenSearch(sorter: String, searcher: String) {
        println("\nStart searching ($sorter sort + $searcher search)...")
        val startTime = System.currentTimeMillis()
        val sortResult = sortMethod.getVal(sorter).invoke(startTime)
        var sortTimeMessage = getExecutionTimeFormatted(startTime)
        val searchStartTime = System.currentTimeMillis()
        val searchResult = if (sortResult == SortStatus.TOO_LONG_EXECUTION) {
            sortTimeMessage += " - STOPPED, moved to linear search"
            linearSearch(phoneBook)
        } else {
            searchMethod.getVal(searcher).invoke()
        }
        printResult(searchResult, startTime)
        println("Sorting time: $sortTimeMessage")
        println("Searching time: ${getExecutionTimeFormatted(searchStartTime)}")
    }

    fun startHashTableSearch(): MutableList<String> {
        println("\nStart searching (hash table)...")
        val startTime = System.currentTimeMillis()
        val hashMap = createHashTable(phoneBook.toList())
        val creationHashMapTime = getExecutionTimeFormatted(startTime)
        val result = mutableListOf<String>()
        val searchStartTime = System.currentTimeMillis()
        for (item in searchItems) {
            val index = hashTableSearchItem(hashMap, item)
            if (index != -1) result.add(phoneBook[index])
        }
        printResult(result, startTime)
        println("Creating time: $creationHashMapTime")
        println("Searching time: ${getExecutionTimeFormatted(searchStartTime)}")
        return result
    }


    private fun sortListBubble(starTime: Long): SortStatus {
        var iteration = 0
        while (iteration < phoneBook.lastIndex - 1) {
            for (i in 0 until phoneBook.lastIndex - iteration++) {
                if (isTooLong(starTime)) return SortStatus.TOO_LONG_EXECUTION
                if (phoneBook[i].getName() > phoneBook[i + 1].getName()) {
                    phoneBook.swap(i, i + 1)
                }
            }
        }
        return SortStatus.FINISHED
    }

    private fun startQuickSort(starTime: Long) = quickSort(starTime)

    private fun quickSort(starTime: Long, low: Int = 0, high: Int = phoneBook.lastIndex): SortStatus {
        if (low >= high) return SortStatus.FINISHED
        val pivot = phoneBook[high].getName()
        var prevLow = low - 1
        for (i in low..high) {
            if (isTooLong(starTime)) return SortStatus.TOO_LONG_EXECUTION
            if (phoneBook[i].getName() <= pivot) {
                if (i != ++prevLow) phoneBook.swap(prevLow, i)
            }
        }
        quickSort(starTime, low, prevLow - 1)
        quickSort(starTime, prevLow + 1, high)
        return SortStatus.FINISHED
    }

    private fun createHashTable(list: List<String>): HashMap<String, Int> {
        val hashMap = HashMap<String, Int>()
        for ((index, string) in list.withIndex()) {
            val name = string.substringAfter(" ")
            hashMap[name] = index
        }
        return hashMap
    }


    private fun hashTableSearchItem(hashMap: HashMap<String, Int>, target: String): Int {
        // Search for name in hash table
        val index = hashMap[target]
        return index ?: -1
    }

    fun linearSearchFirstRun(): MutableList<String> {
        println("Start searching (linear search)...")
        val startTime = System.currentTimeMillis()
        val result = linearSearch(phoneBook)
        maxFunctionExecutionTime = calculateDuration(startTime) * sortToSearchRatio
        printResult(result, startTime)
        return result
    }

    private fun startLinearSearch() = linearSearch(phoneBook)

    private fun linearSearch(phoneBook: Array<String>): MutableList<String> {
        val result = mutableListOf<String>()
        items@ for (item in searchItems) {
            for (entry in phoneBook) {
                if (item in entry) {
                    result.add(entry)
                    continue@items
                }
            }
        }
        return result
    }

    private fun startJumpSearch(): MutableList<String> {
        val result = mutableListOf<String>()
        for (item in searchItems) {
            val index = jumpSearchItemRecursive(phoneBook, item)
            if (index != -1) result.add(phoneBook[index])
        }
        return result
    }

    private fun jumpSearchItemRecursive(array: Array<String>, item: String): Int {
        val jump = sqrt(array.size.toDouble()).toInt()
        var i = 0
        while (i <= array.lastIndex) {
            when {
                array[i].getName() == item -> return i
                array[i].getName() > item -> {
                    val range = i - jump + 1 until i
                    val result = jumpSearchItemRecursive(array.sliceArray(range), item)
                    return if (result != -1) result + range.first else result
                }

                else -> i += (if (i + jump > array.lastIndex) 1 else jump)
            }
        }
        return -1
    }

    private fun startBinarySearch(): MutableList<String> {
        val result = mutableListOf<String>()
        for (item in searchItems) {
            val index = binarySearchItem(phoneBook, item)
            if (index != -1) result.add(phoneBook[index])
        }
        return result
    }

    private fun binarySearchItem(array: Array<String>, item: String): Int {
        var low = 0
        var middle: Int
        var high = array.lastIndex
        while (low <= high) {
            middle = (low + high) / 2
            if (array[middle].getName() == item) return middle
            if (array[middle].getName() < item) {
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return -1
    }

    private fun getExecutionTimeFormatted(startTimeMillis: Long): String {
        val duration = calculateDuration(startTimeMillis)
        return String.format("%1\$tM min. %1\$tS sec. %1\$tL ms.", (duration))
    }

    private fun printResult(result: List<String>, startTime: Long, prefixMessage: String = "Time taken:") {
        val duration = getExecutionTimeFormatted(startTime)
        println("Found ${result.size} / ${searchItems.size} entries. $prefixMessage $duration")
    }

    private fun calculateDuration(startTimeMillis: Long) = System.currentTimeMillis() - startTimeMillis
    private fun isTooLong(startTimeMillis: Long) = calculateDuration(startTimeMillis) > maxFunctionExecutionTime
    private fun String.getName() = this.substringAfter(' ')
    private fun <T> Array<T>.swap(i: Int, j: Int) {
        this[i] = this[j].also { this[j] = this[i] }
    }

    private fun <String, V> Map<String, V>.getVal(key: String): V = this[key]!!
}