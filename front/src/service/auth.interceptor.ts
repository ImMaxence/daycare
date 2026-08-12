import { HttpInterceptorFn } from '@angular/common/http';
import { LOCAL_STORAGE_TOKEN_KEY } from '../utils/constants';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

    const token = localStorage.getItem(LOCAL_STORAGE_TOKEN_KEY);

    if (token) {
        const clonedRequest = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });

        return next(clonedRequest);
    }

    return next(req);
};