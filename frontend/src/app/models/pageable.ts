export interface Pageable {
    page?: number,
    size?: number,
    sort?: string[]
}

export interface Page<T> {
    content: T[],
    page: PageInfo
}

export interface PageInfo {
    size: number,
    number: number,
    totalElements: number,
    totalPages: number
}

export function getParams(pageable: Pageable): URLSearchParams {
    const params = new URLSearchParams()
    if (pageable.page != undefined) {
      params.append('page', pageable.page.toString())
    }
    if (pageable.size != undefined) {
      params.append('size', pageable.size.toString())
    }
    if (pageable.sort != undefined && pageable.sort.length > 0) {
      pageable.sort.forEach(i => params.append('sort', i))
    }
    return params
}