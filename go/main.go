package main

// adapted from cffi_dist from the tls-client repo
// i do not like cgo

/*
#include <jni.h>
#include <stdlib.h>
static inline const char * JNI_GetStringUTFChars(JNIEnv *env, jstring str, jboolean * isCopy) { return (*env)->GetStringUTFChars(env, str, isCopy); }
static inline void JNI_ReleaseStringUTFChars(JNIEnv *env, jstring str, const char * chars) { (*env)->ReleaseStringUTFChars(env, str, chars); }
static inline jstring JNI_NewStringUTF(JNIEnv *env, const char *utf) { return (*env)->NewStringUTF(env, utf); }

static inline char* JNI_GetByteArrayElements(JNIEnv *env, jbyteArray array, jboolean *isCopy) { return (char*) (*env)->GetByteArrayElements(env, array, isCopy); }
static inline void JNI_ReleaseByteArrayElements(JNIEnv *env, jbyteArray array, char* elems, jint mode) { (*env)->ReleaseByteArrayElements(env, array, (jbyte*) elems, mode); }
static inline jsize JNI_GetArrayLength(JNIEnv * env, jarray array) { return (*env)->GetArrayLength(env, array); }
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"net/url"
	"sync"
	"unsafe"

	tls_client_cffi_src "http_client_bindings/cffi_src"

	http "github.com/bogdanfinn/fhttp"
	"github.com/google/uuid"
)

var (
	unsafePointers    = make(map[string]*C.char)
	unsafePointersLck = sync.Mutex{}
)

func freeMemoryImpl(responseId string) {
	unsafePointersLck.Lock()
	defer unsafePointersLck.Unlock()
	ptr, ok := unsafePointers[responseId]
	if !ok {
		return
	}
	C.free(unsafe.Pointer(ptr))
	delete(unsafePointers, responseId)
}

//export freeMemory
func freeMemory(responseId *C.char) {
	responseIdString := C.GoString(responseId)
	freeMemoryImpl(responseIdString)
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_freeMemory
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_freeMemory(env *C.JNIEnv, _cls C.jclass, responseIdStr C.jstring) {
	responseIdChars := C.JNI_GetStringUTFChars(env, responseIdStr, nil)
	defer C.JNI_ReleaseStringUTFChars(env, responseIdStr, responseIdChars)
	responseId := C.GoString(responseIdChars)
	freeMemoryImpl(responseId)
}

//export destroyAll
func destroyAll() *C.char {
	tls_client_cffi_src.ClearSessionCache()
	out := tls_client_cffi_src.DestroyOutput{
		Id:      uuid.New().String(),
		Success: true,
	}
	jsonResponse, marshallError := json.Marshal(out)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse("", false, clientErr)
	}
	responseString := C.CString(string(jsonResponse))
	unsafePointersLck.Lock()
	unsafePointers[out.Id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_destroyAll
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_destroyAll(env *C.JNIEnv, _cls C.jclass) C.jstring {
	response := destroyAll()
	responseJstring := C.JNI_NewStringUTF(env, response)
	return responseJstring
}

func destroySessionImpl(destroySessionParamsJson string) *C.char {
	destroySessionInput := tls_client_cffi_src.DestroySessionInput{}
	marshallError := json.Unmarshal([]byte(destroySessionParamsJson), &destroySessionInput)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse("", false, clientErr)
	}
	tls_client_cffi_src.RemoveSession(destroySessionInput.SessionId)
	out := tls_client_cffi_src.DestroyOutput{
		Id:      uuid.New().String(),
		Success: true,
	}
	jsonResponse, marshallError := json.Marshal(out)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse(destroySessionInput.SessionId, true, clientErr)
	}
	responseString := C.CString(string(jsonResponse))
	unsafePointersLck.Lock()
	unsafePointers[out.Id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

//export destroySession
func destroySession(destroySessionParams *C.char) *C.char {
	destroySessionParamsJson := C.GoString(destroySessionParams)
	return destroySessionImpl(destroySessionParamsJson)
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_destroySession
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_destroySession(env *C.JNIEnv, _cls C.jclass, sessionIdJstring C.jstring) {
	sessionIdChars := C.JNI_GetStringUTFChars(env, sessionIdJstring, nil)
	defer C.JNI_ReleaseStringUTFChars(env, sessionIdJstring, sessionIdChars)
	sessionId := C.GoString(sessionIdChars)
	tls_client_cffi_src.RemoveSession(sessionId)
}

func getCookiesFromSessionImpl(getCookiesParamsJson string) *C.char {
	cookiesInput := tls_client_cffi_src.GetCookiesFromSessionInput{}
	marshallError := json.Unmarshal([]byte(getCookiesParamsJson), &cookiesInput)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse("", false, clientErr)
	}
	tlsClient, err := tls_client_cffi_src.GetClient(cookiesInput.SessionId)
	if err != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(err)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	u, parsErr := url.Parse(cookiesInput.Url)
	if parsErr != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(parsErr)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	cookies := tlsClient.GetCookies(u)
	out := tls_client_cffi_src.CookiesFromSessionOutput{
		Id:      uuid.New().String(),
		Cookies: transformCookies(cookies),
	}
	jsonResponse, marshallError := json.Marshal(out)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	responseString := C.CString(string(jsonResponse))
	unsafePointersLck.Lock()
	unsafePointers[out.Id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

//export getCookiesFromSession
func getCookiesFromSession(getCookiesParams *C.char) *C.char {
	getCookiesParamsJson := C.GoString(getCookiesParams)
	return getCookiesFromSessionImpl((getCookiesParamsJson))
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_getCookiesFromSession
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_getCookiesFromSession(env *C.JNIEnv, _cls C.jclass, getCookiesParamsJstring C.jstring) C.jstring {
	getCookiesParamsChars := C.JNI_GetStringUTFChars(env, getCookiesParamsJstring, nil)
	defer C.JNI_ReleaseStringUTFChars(env, getCookiesParamsJstring, getCookiesParamsChars)
	getCookiesParams := C.GoString(getCookiesParamsChars)
	responseString := getCookiesFromSessionImpl(getCookiesParams)
	return C.JNI_NewStringUTF(env, responseString)
}

func addCookiesToSessionImpl(addCookiesParamsJson string) *C.char {
	cookiesInput := tls_client_cffi_src.AddCookiesToSessionInput{}
	marshallError := json.Unmarshal([]byte(addCookiesParamsJson), &cookiesInput)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse("", false, clientErr)
	}
	tlsClient, err := tls_client_cffi_src.GetClient(cookiesInput.SessionId)
	if err != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(err)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	u, parsErr := url.Parse(cookiesInput.Url)
	if parsErr != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(parsErr)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	tlsClient.SetCookies(u, buildCookies(cookiesInput.Cookies))
	allCookies := tlsClient.GetCookies(u)
	out := tls_client_cffi_src.CookiesFromSessionOutput{
		Id:      uuid.New().String(),
		Cookies: transformCookies(allCookies),
	}
	jsonResponse, marshallError := json.Marshal(out)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponse(cookiesInput.SessionId, true, clientErr)
	}
	responseString := C.CString(string(jsonResponse))
	unsafePointersLck.Lock()
	unsafePointers[out.Id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

//export addCookiesToSession
func addCookiesToSession(addCookiesParams *C.char) *C.char {
	addCookiesParamsJson := C.GoString(addCookiesParams)
	return addCookiesToSessionImpl(addCookiesParamsJson)
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_addCookiesToSession
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_addCookiesToSession(env *C.JNIEnv, _cls C.jclass, addCookiesParamsJstring C.jstring) C.jstring {
	addCookiesParamsChars := C.JNI_GetStringUTFChars(env, addCookiesParamsJstring, nil)
	defer C.JNI_ReleaseStringUTFChars(env, addCookiesParamsJstring, addCookiesParamsChars)
	addCookiesParams := C.GoString(addCookiesParamsChars)
	responseString := addCookiesToSessionImpl(addCookiesParams)
	return C.JNI_NewStringUTF(env, responseString)
}

func requestImpl(requestParamsJson string) (string, string) {
	requestInput := tls_client_cffi_src.RequestInput{}
	marshallError := json.Unmarshal([]byte(requestParamsJson), &requestInput)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponseImpl("", false, clientErr)
	}
	tlsClient, sessionId, withSession, err := tls_client_cffi_src.CreateClient(requestInput)
	if err != nil {
		return handleErrorResponseImpl(sessionId, withSession, err)
	}
	req, err := tls_client_cffi_src.BuildRequest(requestInput)
	if err != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(err)
		return handleErrorResponseImpl(sessionId, withSession, clientErr)
	}
	cookies := buildCookies(requestInput.RequestCookies)
	if tlsClient.GetCookieJar() != nil && len(cookies) > 0 {
		tlsClient.SetCookies(req.URL, cookies)
	} else {
		for _, cookie := range cookies {
			req.AddCookie(cookie)
		}
	}
	resp, reqErr := tlsClient.Do(req)
	if reqErr != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(fmt.Errorf("failed to do request: %w", reqErr))
		return handleErrorResponseImpl(sessionId, withSession, clientErr)
	}
	if resp == nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(fmt.Errorf("response is nil"))
		return handleErrorResponseImpl(sessionId, withSession, clientErr)
	}
	targetCookies := tlsClient.GetCookies(resp.Request.URL)
	response, err := tls_client_cffi_src.BuildResponse(sessionId, withSession, resp, targetCookies, requestInput)
	if err != nil {
		return handleErrorResponseImpl(sessionId, withSession, err)
	}
	jsonResponse, marshallError := json.Marshal(response)
	if marshallError != nil {
		clientErr := tls_client_cffi_src.NewTLSClientError(marshallError)
		return handleErrorResponseImpl(sessionId, withSession, clientErr)
	}
	return string(jsonResponse), response.Id
}

//export request
func request(requestParams *C.char) *C.char {
	requestParamsJson := C.GoString(requestParams)
	jsonResponse, id := requestImpl(requestParamsJson)
	
	responseString := C.CString(jsonResponse)
	unsafePointersLck.Lock()
	unsafePointers[id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

//export Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_request
func Java_com_github_bogdanfinn_tlsclient_TLSClientJNI_request(env *C.JNIEnv, _cls C.jclass, requestParamsJbytearray C.jbyteArray) C.jstring {
	requestParamsLength := C.JNI_GetArrayLength(env, C.jarray(requestParamsJbytearray))
	requestParamsChars := C.JNI_GetByteArrayElements(env, requestParamsJbytearray, nil)
	defer C.JNI_ReleaseByteArrayElements(env, requestParamsJbytearray, requestParamsChars, 0)

	requestParams := C.GoStringN(requestParamsChars, C.int(requestParamsLength))
	responseString, _ := requestImpl(requestParams)
	response := C.CString(responseString)
	defer C.free(unsafe.Pointer(response))
	return C.JNI_NewStringUTF(env, response)
}

func handleErrorResponseImpl(sessionId string, withSession bool, err *tls_client_cffi_src.TLSClientError) (string, string) {
	response := tls_client_cffi_src.Response{
		Id:      uuid.New().String(),
		Status:  0,
		Body:    err.Error(),
		Headers: nil,
		Cookies: nil,
	}
	if withSession {
		response.SessionId = sessionId
	}
	jsonResponse, marshallError := json.Marshal(response)
	if marshallError != nil {
		return marshallError.Error(), response.Id
	}
	return string(jsonResponse), response.Id
}

func handleErrorResponse(sessionId string, withSession bool, err *tls_client_cffi_src.TLSClientError) *C.char {
	jsonResponse, id := handleErrorResponseImpl(sessionId, withSession, err)

	responseString := C.CString(jsonResponse)
	unsafePointersLck.Lock()
	unsafePointers[id] = responseString
	unsafePointersLck.Unlock()
	return responseString
}

func buildCookies(cookies []tls_client_cffi_src.Cookie) []*http.Cookie {
	var ret []*http.Cookie
	for _, cookie := range cookies {
		ret = append(ret, &http.Cookie{
			Name:    cookie.Name,
			Value:   cookie.Value,
			Path:    cookie.Path,
			Domain:  cookie.Domain,
			Expires: cookie.Expires.Time,
			MaxAge:  cookie.MaxAge,
		})
	}
	return ret
}

func transformCookies(cookies []*http.Cookie) []tls_client_cffi_src.Cookie {
	var ret []tls_client_cffi_src.Cookie
	for _, cookie := range cookies {
		ret = append(ret, tls_client_cffi_src.Cookie{
			Name:   cookie.Name,
			Value:  cookie.Value,
			Path:   cookie.Path,
			Domain: cookie.Domain,
			MaxAge: cookie.MaxAge,
			Expires: tls_client_cffi_src.Timestamp{
				Time: cookie.Expires,
			},
		})
	}
	return ret
}

func main() {
}
