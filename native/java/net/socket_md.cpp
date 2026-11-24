// java.net.SocketOps: non-blocking POSIX socket primitives.

#include "stdafx.h"
#include "jni_impl.h"

#include <cerrno>
#include <cstring>

#if defined(_WIN32)

#include <winsock2.h>
#include <ws2tcpip.h>

namespace {

int last_error()
{
	return WSAGetLastError();
}

bool would_block(int error)
{
	return error == WSAEWOULDBLOCK || error == WSAEINPROGRESS;
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL Java_java_net_SocketOps_socket0(JNIEnv *, jclass)
{
	SOCKET s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	return s == INVALID_SOCKET ? -1 : static_cast<jint>(s);
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_close0(JNIEnv *, jclass, jint fd)
{
	if (fd >= 0)
	{
		closesocket(static_cast<SOCKET>(fd));
	}
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_setOpt0(JNIEnv *, jclass, jint fd,
	jint option, jint value)
{
	if (fd < 0) return -1;
	int level = SOL_SOCKET;
	int name = 0;
	switch (option)
	{
	case 1: level = IPPROTO_TCP; name = TCP_NODELAY; break;
	case 2: name = SO_KEEPALIVE; break;
	case 3: name = SO_REUSEADDR; break;
	case 4: name = SO_RCVBUF; break;
	case 5: name = SO_SNDBUF; break;
	case 6: name = SO_LINGER; break;
	case 7: name = SO_OOBINLINE; break;
	default: return -1;
	}
	int result;
	if (option == 6)
	{
		struct linger linger_value;
		linger_value.l_onoff = value >= 0 ? 1 : 0;
		linger_value.l_linger = value < 0 ? 0 : value;
		result = setsockopt(static_cast<SOCKET>(fd), level, name,
			reinterpret_cast<const char*>(&linger_value), sizeof(linger_value));
	}
	else
	{
		result = setsockopt(static_cast<SOCKET>(fd), level, name,
			reinterpret_cast<const char*>(&value), sizeof(value));
	}
	return result == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_getOpt0(JNIEnv *, jclass, jint fd,
	jint option)
{
	if (fd < 0) return -1;
	int level = SOL_SOCKET;
	int name = 0;
	switch (option)
	{
	case 1: level = IPPROTO_TCP; name = TCP_NODELAY; break;
	case 2: name = SO_KEEPALIVE; break;
	case 3: name = SO_REUSEADDR; break;
	case 4: name = SO_RCVBUF; break;
	case 5: name = SO_SNDBUF; break;
	case 6: name = SO_LINGER; break;
	case 7: name = SO_OOBINLINE; break;
	default: return -1;
	}
	int value = 0;
	int length = sizeof(value);
	if (getsockopt(static_cast<SOCKET>(fd), level, name,
		reinterpret_cast<char*>(&value), &length) != 0)
	{
		return -1;
	}
	return value;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_bind0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jint port)
{
	if (fd < 0 || !address) return -1;
	jbyte bytes[4];
	env->GetByteArrayRegion(address, 0, 4, bytes);
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(static_cast<unsigned short>(port));
	memcpy(&addr.sin_addr, bytes, 4);
	return bind(static_cast<SOCKET>(fd), reinterpret_cast<struct sockaddr*>(&addr),
		sizeof(addr)) == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_listen0(JNIEnv *, jclass, jint fd,
	jint backlog)
{
	if (fd < 0) return -1;
	return listen(static_cast<SOCKET>(fd), backlog) == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_accept0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jintArray port)
{
	if (fd < 0) return -2;
	struct sockaddr_in addr;
	int length = sizeof(addr);
	SOCKET s = accept(static_cast<SOCKET>(fd),
		reinterpret_cast<struct sockaddr*>(&addr), &length);
	if (s == INVALID_SOCKET)
	{
		return would_block(last_error()) ? -1 : -2;
	}
	u_long nonblocking = 1;
	ioctlsocket(s, FIONBIO, &nonblocking);
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	if (port)
	{
		jint p = ntohs(addr.sin_port);
		env->SetIntArrayRegion(port, 0, 1, &p);
	}
	return static_cast<jint>(s);
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_connect0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jint port)
{
	if (fd < 0 || !address) return -2;
	jbyte bytes[4];
	env->GetByteArrayRegion(address, 0, 4, bytes);
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(static_cast<unsigned short>(port));
	memcpy(&addr.sin_addr, bytes, 4);
	if (connect(static_cast<SOCKET>(fd), reinterpret_cast<struct sockaddr*>(&addr),
		sizeof(addr)) == 0)
	{
		return 0;
	}
	int error = last_error();
	if (error == WSAEWOULDBLOCK || error == WSAEINPROGRESS || error == WSAEINVAL)
	{
		return -1;
	}
	return -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_finishConnect0(JNIEnv *env, jclass,
	jint fd, jbyteArray address, jintArray port)
{
	if (fd < 0) return -2;
	int error = 0;
	int length = sizeof(error);
	if (getsockopt(static_cast<SOCKET>(fd), SOL_SOCKET, SO_ERROR,
		reinterpret_cast<char*>(&error), &length) != 0)
	{
		return -2;
	}
	if (error == WSAEWOULDBLOCK || error == WSAEINPROGRESS)
	{
		return -1;
	}
	if (error != 0)
	{
		return -2;
	}
	struct sockaddr_in addr;
	int addr_length = sizeof(addr);
	if (getpeername(static_cast<SOCKET>(fd), reinterpret_cast<struct sockaddr*>(&addr),
		&addr_length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	if (port)
	{
		jint p = ntohs(addr.sin_port);
		env->SetIntArrayRegion(port, 0, 1, &p);
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_read0(JNIEnv *env, jclass, jint fd,
	jbyteArray buffer, jint offset, jint length)
{
	if (fd < 0 || !buffer || length <= 0) return -2;
	std::vector<jbyte> bytes(static_cast<size_t>(length));
	int n = recv(static_cast<SOCKET>(fd), reinterpret_cast<char*>(&bytes[0]), length, 0);
	if (n > 0)
	{
		env->SetByteArrayRegion(buffer, offset, n, &bytes[0]);
		return n;
	}
	if (n == 0)
	{
		return 0;
	}
	return would_block(last_error()) ? -1 : -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_write0(JNIEnv *env, jclass, jint fd,
	jbyteArray buffer, jint offset, jint length)
{
	if (fd < 0 || !buffer || length <= 0) return -2;
	std::vector<jbyte> bytes(static_cast<size_t>(length));
	env->GetByteArrayRegion(buffer, offset, length, &bytes[0]);
	int n = send(static_cast<SOCKET>(fd), reinterpret_cast<const char*>(&bytes[0]),
		length, 0);
	if (n > 0)
	{
		return n;
	}
	return would_block(last_error()) ? -1 : -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_localAddress0(JNIEnv *env, jclass,
	jint fd, jbyteArray address)
{
	if (fd < 0) return -1;
	struct sockaddr_in addr;
	int length = sizeof(addr);
	if (getsockname(static_cast<SOCKET>(fd), reinterpret_cast<struct sockaddr*>(&addr),
		&length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	return ntohs(addr.sin_port);
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_peerAddress0(JNIEnv *env, jclass,
	jint fd, jbyteArray address)
{
	if (fd < 0) return -1;
	struct sockaddr_in addr;
	int length = sizeof(addr);
	if (getpeername(static_cast<SOCKET>(fd), reinterpret_cast<struct sockaddr*>(&addr),
		&length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	return ntohs(addr.sin_port);
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_shutdown0(JNIEnv *, jclass, jint fd,
	jint how)
{
	if (fd >= 0)
	{
		shutdown(static_cast<SOCKET>(fd), how);
	}
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_poll0(JNIEnv *env, jclass,
	jintArray fds, jintArray ops, jintArray outOps)
{
	jint count = fds ? env->GetArrayLength(fds) : 0;
	std::vector<jint> fd_values(static_cast<size_t>(count));
	std::vector<jint> op_values(static_cast<size_t>(count));
	std::vector<jint> out_values(static_cast<size_t>(count));
	if (count > 0)
	{
		env->GetIntArrayRegion(fds, 0, count, &fd_values[0]);
		env->GetIntArrayRegion(ops, 0, count, &op_values[0]);
	}
	std::vector<WSAPOLLFD> poll_fds(static_cast<size_t>(count));
	for (jint i = 0; i < count; i++)
	{
		poll_fds[i].fd = fd_values[i];
		poll_fds[i].events = 0;
		if (op_values[i] & 1) poll_fds[i].events |= POLLIN;
		if (op_values[i] & 4) poll_fds[i].events |= POLLOUT;
		if (op_values[i] & 16) poll_fds[i].events |= POLLIN;
		poll_fds[i].revents = 0;
	}
	int ready = count > 0 ? WSAPoll(&poll_fds[0], count, 0) : 0;
	for (jint i = 0; i < count; i++)
	{
		int result = 0;
		if (poll_fds[i].revents & (POLLIN | POLLHUP | POLLERR)) result |= 1;
		if (poll_fds[i].revents & POLLOUT) result |= 4;
		out_values[i] = result;
	}
	if (count > 0 && outOps)
	{
		env->SetIntArrayRegion(outOps, 0, count, &out_values[0]);
	}
	return ready;
}

JNIEXPORT jintArray JNICALL Java_java_net_SocketOps_pipe0(JNIEnv *env, jclass)
{
	return NULL;
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_drain0(JNIEnv *, jclass, jint)
{
}

} // extern "C"

#else // POSIX

#include <arpa/inet.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <poll.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace {

void SetNonBlocking(int fd)
{
	int flags = fcntl(fd, F_GETFL, 0);
	if (flags >= 0)
	{
		fcntl(fd, F_SETFL, flags | O_NONBLOCK);
	}
	fcntl(fd, F_SETFD, FD_CLOEXEC);
}

bool WouldBlock(int error)
{
	return error == EAGAIN || error == EWOULDBLOCK || error == EINPROGRESS;
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL Java_java_net_SocketOps_socket0(JNIEnv *, jclass)
{
	int fd = socket(AF_INET, SOCK_STREAM, 0);
	if (fd < 0)
	{
		return -1;
	}
	SetNonBlocking(fd);
	return fd;
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_close0(JNIEnv *, jclass, jint fd)
{
	if (fd >= 0)
	{
		close(fd);
	}
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_setOpt0(JNIEnv *, jclass, jint fd,
	jint option, jint value)
{
	if (fd < 0) return -1;
	int level = SOL_SOCKET;
	int name = 0;
	switch (option)
	{
	case 1: level = IPPROTO_TCP; name = TCP_NODELAY; break;
	case 2: name = SO_KEEPALIVE; break;
	case 3: name = SO_REUSEADDR; break;
	case 4: name = SO_RCVBUF; break;
	case 5: name = SO_SNDBUF; break;
	case 6: name = SO_LINGER; break;
	case 7: name = SO_OOBINLINE; break;
	default: return -1;
	}
	int result;
	if (option == 6)
	{
		struct linger linger_value;
		linger_value.l_onoff = value >= 0 ? 1 : 0;
		linger_value.l_linger = value < 0 ? 0 : value;
		result = setsockopt(fd, level, name, &linger_value, sizeof(linger_value));
	}
	else
	{
		result = setsockopt(fd, level, name, &value, sizeof(value));
	}
	return result == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_getOpt0(JNIEnv *, jclass, jint fd,
	jint option)
{
	if (fd < 0) return -1;
	int level = SOL_SOCKET;
	int name = 0;
	switch (option)
	{
	case 1: level = IPPROTO_TCP; name = TCP_NODELAY; break;
	case 2: name = SO_KEEPALIVE; break;
	case 3: name = SO_REUSEADDR; break;
	case 4: name = SO_RCVBUF; break;
	case 5: name = SO_SNDBUF; break;
	case 6: name = SO_LINGER; break;
	case 7: name = SO_OOBINLINE; break;
	default: return -1;
	}
	int value = 0;
	socklen_t length = sizeof(value);
	if (getsockopt(fd, level, name, &value, &length) != 0)
	{
		return -1;
	}
	return value;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_bind0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jint port)
{
	if (fd < 0 || !address) return -1;
	jbyte bytes[4];
	env->GetByteArrayRegion(address, 0, 4, bytes);
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(static_cast<unsigned short>(port));
	memcpy(&addr.sin_addr, bytes, 4);
	return ::bind(fd, reinterpret_cast<struct sockaddr*>(&addr), sizeof(addr)) == 0
		? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_listen0(JNIEnv *, jclass, jint fd,
	jint backlog)
{
	if (fd < 0) return -1;
	return listen(fd, backlog) == 0 ? 0 : -1;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_accept0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jintArray port)
{
	if (fd < 0) return -2;
	struct sockaddr_in addr;
	socklen_t length = sizeof(addr);
	int s = accept(fd, reinterpret_cast<struct sockaddr*>(&addr), &length);
	if (s < 0)
	{
		return WouldBlock(errno) ? -1 : -2;
	}
	SetNonBlocking(s);
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	if (port)
	{
		jint p = ntohs(addr.sin_port);
		env->SetIntArrayRegion(port, 0, 1, &p);
	}
	return s;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_connect0(JNIEnv *env, jclass, jint fd,
	jbyteArray address, jint port)
{
	if (fd < 0 || !address) return -2;
	jbyte bytes[4];
	env->GetByteArrayRegion(address, 0, 4, bytes);
	struct sockaddr_in addr;
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_port = htons(static_cast<unsigned short>(port));
	memcpy(&addr.sin_addr, bytes, 4);
	if (connect(fd, reinterpret_cast<struct sockaddr*>(&addr), sizeof(addr)) == 0)
	{
		return 0;
	}
	return WouldBlock(errno) ? -1 : -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_finishConnect0(JNIEnv *env, jclass,
	jint fd, jbyteArray address, jintArray port)
{
	if (fd < 0) return -2;
	int error = 0;
	socklen_t length = sizeof(error);
	if (getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &length) != 0)
	{
		return -2;
	}
	if (error == EINPROGRESS || error == EALREADY)
	{
		return -1;
	}
	if (error != 0)
	{
		return -2;
	}
	struct sockaddr_in addr;
	socklen_t addr_length = sizeof(addr);
	if (getpeername(fd, reinterpret_cast<struct sockaddr*>(&addr), &addr_length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	if (port)
	{
		jint p = ntohs(addr.sin_port);
		env->SetIntArrayRegion(port, 0, 1, &p);
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_read0(JNIEnv *env, jclass, jint fd,
	jbyteArray buffer, jint offset, jint length)
{
	if (fd < 0 || !buffer || length <= 0) return -2;
	std::vector<jbyte> bytes(static_cast<size_t>(length));
	ssize_t n = recv(fd, &bytes[0], static_cast<size_t>(length), 0);
	if (n > 0)
	{
		env->SetByteArrayRegion(buffer, offset, static_cast<jsize>(n), &bytes[0]);
		return static_cast<jint>(n);
	}
	if (n == 0)
	{
		return 0;
	}
	return WouldBlock(errno) ? -1 : -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_write0(JNIEnv *env, jclass, jint fd,
	jbyteArray buffer, jint offset, jint length)
{
	if (fd < 0 || !buffer || length <= 0) return -2;
	std::vector<jbyte> bytes(static_cast<size_t>(length));
	env->GetByteArrayRegion(buffer, offset, length, &bytes[0]);
	ssize_t n = send(fd, &bytes[0], static_cast<size_t>(length), MSG_NOSIGNAL);
	if (n < 0 && errno == ENOTSOCK)
	{
		// Selector wake pipes are plain descriptors, not sockets.
		n = write(fd, &bytes[0], static_cast<size_t>(length));
	}
	if (n > 0)
	{
		return static_cast<jint>(n);
	}
	return WouldBlock(errno) ? -1 : -2;
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_localAddress0(JNIEnv *env, jclass,
	jint fd, jbyteArray address)
{
	if (fd < 0) return -1;
	struct sockaddr_in addr;
	socklen_t length = sizeof(addr);
	if (getsockname(fd, reinterpret_cast<struct sockaddr*>(&addr), &length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	return ntohs(addr.sin_port);
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_peerAddress0(JNIEnv *env, jclass,
	jint fd, jbyteArray address)
{
	if (fd < 0) return -1;
	struct sockaddr_in addr;
	socklen_t length = sizeof(addr);
	if (getpeername(fd, reinterpret_cast<struct sockaddr*>(&addr), &length) != 0)
	{
		return -1;
	}
	if (address)
	{
		env->SetByteArrayRegion(address, 0, 4,
			reinterpret_cast<const jbyte*>(&addr.sin_addr));
	}
	return ntohs(addr.sin_port);
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_shutdown0(JNIEnv *, jclass, jint fd,
	jint how)
{
	if (fd >= 0)
	{
		shutdown(fd, how);
	}
}

JNIEXPORT jint JNICALL Java_java_net_SocketOps_poll0(JNIEnv *env, jclass,
	jintArray fds, jintArray ops, jintArray outOps)
{
	jint count = fds ? env->GetArrayLength(fds) : 0;
	std::vector<jint> fd_values(static_cast<size_t>(count));
	std::vector<jint> op_values(static_cast<size_t>(count));
	std::vector<jint> out_values(static_cast<size_t>(count));
	if (count > 0)
	{
		env->GetIntArrayRegion(fds, 0, count, &fd_values[0]);
		env->GetIntArrayRegion(ops, 0, count, &op_values[0]);
	}
	std::vector<struct pollfd> poll_fds(static_cast<size_t>(count));
	for (jint i = 0; i < count; i++)
	{
		poll_fds[i].fd = fd_values[i];
		poll_fds[i].events = 0;
		if (op_values[i] & 1) poll_fds[i].events |= POLLIN;
		if (op_values[i] & 4) poll_fds[i].events |= POLLOUT;
		if (op_values[i] & 16) poll_fds[i].events |= POLLIN;
		poll_fds[i].revents = 0;
	}
	int ready = count > 0 ? poll(&poll_fds[0], static_cast<nfds_t>(count), 0) : 0;
	for (jint i = 0; i < count; i++)
	{
		int result = 0;
		if (poll_fds[i].revents & (POLLIN | POLLHUP | POLLERR)) result |= 1;
		if (poll_fds[i].revents & POLLOUT) result |= 4;
		out_values[i] = result;
	}
	if (count > 0 && outOps)
	{
		env->SetIntArrayRegion(outOps, 0, count, &out_values[0]);
	}
	return ready;
}

JNIEXPORT jintArray JNICALL Java_java_net_SocketOps_pipe0(JNIEnv *env, jclass)
{
	int pipe_fds[2];
#if defined(__linux__)
	if (pipe2(pipe_fds, O_NONBLOCK | O_CLOEXEC) != 0)
	{
		return NULL;
	}
#else
	if (pipe(pipe_fds) != 0)
	{
		return NULL;
	}
	SetNonBlocking(pipe_fds[0]);
	SetNonBlocking(pipe_fds[1]);
#endif
	jintArray result = env->NewIntArray(2);
	if (!result)
	{
		close(pipe_fds[0]);
		close(pipe_fds[1]);
		return NULL;
	}
	env->SetIntArrayRegion(result, 0, 2, pipe_fds);
	return result;
}

JNIEXPORT void JNICALL Java_java_net_SocketOps_drain0(JNIEnv *, jclass, jint fd)
{
	if (fd < 0) return;
	char buffer[64];
	while (read(fd, buffer, sizeof(buffer)) > 0)
	{
	}
}

} // extern "C"

#endif
