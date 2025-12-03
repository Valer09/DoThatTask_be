package homeaq.dothattask.data
class DataResponse <T>(result: DataResult, message: String, data: T?)
{
    private val _result: DataResult = result
    val result: DataResult
        get() = _result

    private val _message: String = message
    val message: String
        get() = _message

    private val _data: T? = data

    val data: T?
        get() = _data

    fun isSuccessful() = result == DataResult.SUCCESS

    companion object
    {
        fun <T> success(data: T, message : String = "Success"): DataResponse<T> = DataResponse(DataResult.SUCCESS, message, data)
        fun <T> notFound(message: String = "Not Found"): DataResponse<T> = DataResponse(DataResult.NOT_FOUND, message, null)
        fun <T> validationError(message: String): DataResponse<T> = DataResponse(DataResult.VALIDATION_ERROR, message, null)
        fun <T> databaseError(message: String): DataResponse<T> = DataResponse(DataResult.DATABASE_ERROR, message, null)
    }
}