import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { asyncErrorHandler, tryCatch, ErrorHandler, AppError } from '@/utils/errorHandler'

/**
 * 请求状态管理 Hook
 */
export function useRequest<T = any>(
  requestFn: (...args: any[]) => Promise<T>,
  options?: {
    immediate?: boolean
    onSuccess?: (data: T) => void
    onError?: (error: any) => void
    showErrorMessage?: boolean
    successMessage?: string
  }
) {
  const loading = ref(false)
  const error = ref<AppError | null>(null)
  const data = ref<T | null>(null)

  const execute = async (...args: any[]) => {
    loading.value = true
    error.value = null

    const [result, err] = await tryCatch(
      () => requestFn(...args),
      {
        showError: options?.showErrorMessage !== false
      }
    )

    loading.value = false

    if (err) {
      error.value = err
      options?.onError?.(err)
      return null
    }

    data.value = result
    
    if (options?.successMessage) {
      ElMessage.success(options.successMessage)
    }
    
    options?.onSuccess?.(result as T)
    return result
  }

  // 立即执行
  if (options?.immediate) {
    execute()
  }

  return {
    loading,
    error,
    data,
    execute
  }
}

/**
 * 表单提交 Hook
 */
export function useFormSubmit<T = any>(
  submitFn: (formData: any) => Promise<T>,
  options?: {
    validate?: () => Promise<boolean>
    onSuccess?: (data: T) => void
    onError?: (error: any) => void
    successMessage?: string
  }
) {
  const submitting = ref(false)
  const error = ref<AppError | null>(null)

  const submit = async (formData: any) => {
    // 表单验证
    if (options?.validate) {
      try {
        const valid = await options.validate()
        if (!valid) {
          return false
        }
      } catch (validationError) {
        ErrorHandler.handleValidationError(
          validationError?.message || '表单验证失败'
        )
        return false
      }
    }

    submitting.value = true
    error.value = null

    const [result, err] = await tryCatch(
      () => submitFn(formData),
      {
        showError: true
      }
    )

    submitting.value = false

    if (err) {
      error.value = err
      options?.onError?.(err)
      return false
    }

    if (options?.successMessage) {
      ElMessage.success(options.successMessage)
    }

    options?.onSuccess?.(result as T)
    return true
  }

  return {
    submitting,
    error,
    submit
  }
}

/**
 * 列表加载 Hook
 */
export function useList<T = any>(
  fetchFn: (params?: any) => Promise<{ list: T[]; total?: number }>,
  options?: {
    immediate?: boolean
    pagination?: {
      page?: number
      pageSize?: number
    }
  }
) {
  const loading = ref(false)
  const list = ref<T[]>([])
  const total = ref(0)
  const error = ref<AppError | null>(null)
  const page = ref(options?.pagination?.page || 1)
  const pageSize = ref(options?.pagination?.pageSize || 10)

  const fetch = async (params?: any) => {
    loading.value = true
    error.value = null

    const [result, err] = await tryCatch(
      () => fetchFn({
        page: page.value,
        pageSize: pageSize.value,
        ...params
      }),
      {
        showError: true
      }
    )

    loading.value = false

    if (err) {
      error.value = err
      return
    }

    if (result) {
      list.value = result.list || []
      total.value = result.total || 0
    }
  }

  const refresh = () => fetch()

  const reset = () => {
    page.value = 1
    fetch()
  }

  const handlePageChange = (newPage: number) => {
    page.value = newPage
    fetch()
  }

  const handleSizeChange = (newSize: number) => {
    pageSize.value = newSize
    page.value = 1
    fetch()
  }

  if (options?.immediate) {
    fetch()
  }

  return {
    loading,
    list,
    total,
    error,
    page,
    pageSize,
    fetch,
    refresh,
    reset,
    handlePageChange,
    handleSizeChange
  }
}

/**
 * 删除确认 Hook
 */
export function useDelete(
  deleteFn: (id: string | number) => Promise<any>,
  options?: {
    confirmMessage?: string
    onSuccess?: () => void
    successMessage?: string
  }
) {
  const deleting = ref(false)

  const confirmDelete = async (id: string | number) => {
    const { ElMessageBox } = await import('element-plus')
    
    try {
      await ElMessageBox.confirm(
        options?.confirmMessage || '确定要删除这条数据吗?此操作不可撤销。',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      deleting.value = true

      const [, err] = await tryCatch(
        () => deleteFn(id),
        {
          showError: true
        }
      )

      deleting.value = false

      if (!err) {
        ElMessage.success(options?.successMessage || '删除成功')
        options?.onSuccess?.()
        return true
      }

      return false
    } catch (error) {
      // 用户取消删除
      return false
    }
  }

  return {
    deleting,
    confirmDelete
  }
}

/**
 * 异步操作 Hook
 */
export function useAsyncAction<T = any>(
  actionFn: (...args: any[]) => Promise<T>,
  options?: {
    onSuccess?: (data: T) => void
    onError?: (error: any) => void
    successMessage?: string
    showError?: boolean
  }
) {
  const executing = ref(false)
  const error = ref<AppError | null>(null)

  const execute = asyncErrorHandler(
    async (...args: any[]) => {
      executing.value = true
      error.value = null

      try {
        const result = await actionFn(...args)
        
        if (options?.successMessage) {
          ElMessage.success(options.successMessage)
        }
        
        options?.onSuccess?.(result)
        return result
      } catch (err: any) {
        error.value = err instanceof AppError ? err : new AppError(err.message)
        options?.onError?.(err)
        throw err
      } finally {
        executing.value = false
      }
    },
    {
      showError: options?.showError !== false
    }
  )

  return {
    executing,
    error,
    execute
  }
}
