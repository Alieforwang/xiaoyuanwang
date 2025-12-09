#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
深澜校园网认证系统 - Python实现
支持登录、注销、保持在线功能
"""

import re
import json
import time
import hashlib
import base64
import struct
import requests
from typing import Optional, Dict, Any


class XXTEACipher:
    """XXTEA加密算法实现"""

    DELTA = 0x9E3779B9

    @staticmethod
    def _int32(x):
        """转换为32位无符号整数"""
        return x & 0xFFFFFFFF

    @classmethod
    def encrypt(cls, data: str, key: str) -> bytes:
        """
        XXTEA加密
        :param data: 明文字符串
        :param key: 密钥字符串
        :return: 加密后的字节数据
        """
        if not data:
            return b''

        # 只使用密钥的前16个字符（128位）
        key = key[:16]

        v = cls._str2long(data, True)
        k = cls._str2long(key, False)

        # 确保密钥恰好是4个整数（16字节）
        if len(k) < 4:
            k = k + [0] * (4 - len(k))
        elif len(k) > 4:
            k = k[:4]

        n = len(v) - 1
        z = v[n]
        y = v[0]
        q = 6 + 52 // (n + 1)
        sum_val = 0

        while q > 0:
            sum_val = cls._int32(sum_val + cls.DELTA)
            e = (sum_val >> 2) & 3

            for p in range(n):
                y = v[p + 1]
                # Match JavaScript implementation: m = (z>>5 ^ y<<2) + (y>>3 ^ z<<4 ^ d ^ y) + (k[p&3^e] ^ z)
                mx = cls._int32(z >> 5) ^ cls._int32(y << 2)
                mx = cls._int32(mx + (cls._int32(y >> 3) ^ cls._int32(z << 4) ^ sum_val ^ y))
                mx = cls._int32(mx + (k[(p & 3) ^ e] ^ z))
                z = v[p] = cls._int32(v[p] + mx)

            y = v[0]
            # Match JavaScript implementation: m = (z>>5 ^ y<<2) + (y>>3 ^ z<<4 ^ d ^ y) + (k[n&3^e] ^ z)
            mx = cls._int32(z >> 5) ^ cls._int32(y << 2)
            mx = cls._int32(mx + (cls._int32(y >> 3) ^ cls._int32(z << 4) ^ sum_val ^ y))
            mx = cls._int32(mx + (k[(n & 3) ^ e] ^ z))
            z = v[n] = cls._int32(v[n] + mx)

            q -= 1

        return cls._long2str(v, False)

    @classmethod
    def _str2long(cls, s: str, include_length: bool) -> list:
        """字符串转长整数数组"""
        length = len(s)
        v = []

        for i in range(0, length, 4):
            val = (
                ord(s[i]) |
                (ord(s[i + 1]) << 8 if i + 1 < length else 0) |
                (ord(s[i + 2]) << 16 if i + 2 < length else 0) |
                (ord(s[i + 3]) << 24 if i + 3 < length else 0)
            )
            v.append(val & 0xFFFFFFFF)

        if include_length:
            v.append(length)

        return v

    @classmethod
    def _long2str(cls, v: list, include_length: bool) -> bytes:
        """长整数数组转字符串"""
        length = len(v)
        n = (length - 1) << 2

        if include_length:
            m = v[length - 1]
            if m < n - 3 or m > n:
                return b''
            n = m

        result = []
        for i in range(length):
            result.append(struct.pack('<I', v[i] & 0xFFFFFFFF))

        return b''.join(result)[:n] if include_length else b''.join(result)


class SrunPortal:
    """深澜Portal认证客户端"""

    def __init__(self, base_url: str = "http://10.0.100.100"):
        """
        初始化
        :param base_url: Portal服务器地址
        """
        self.base_url = base_url.rstrip('/')
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })

    def _parse_jsonp(self, text: str) -> Dict[str, Any]:
        """解析JSONP响应"""
        match = re.search(r'\((\{.*\})\)', text)
        if match:
            return json.loads(match.group(1))
        return json.loads(text)

    def _get_md5(self, password: str, token: str) -> str:
        """计算MD5哈希"""
        return hashlib.md5(f"{password}{token}".encode()).hexdigest()

    def _get_sha1(self, text: str) -> str:
        """计算SHA1哈希"""
        return hashlib.sha1(text.encode()).hexdigest()

    def _encode_user_info(self, info: Dict[str, Any], token: str) -> str:
        """
        加密用户信息
        :param info: 用户信息字典
        :param token: 加密令牌
        :return: 加密后的字符串
        """
        # 自定义Base64字母表
        custom_alphabet = 'LVoJPiCN2R8G90yg+hmFHuacZ1OWMnrsSTXkYpUq/3dlbfKwv6xztjI7DeBE45QA'

        # JSON序列化
        json_str = json.dumps(info, separators=(',', ':'))

        # XXTEA加密
        encrypted = XXTEACipher.encrypt(json_str, token)

        # Base64编码（使用自定义字母表）
        standard_b64 = base64.b64encode(encrypted).decode()

        # 替换字符
        standard_alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
        trans_table = str.maketrans(standard_alphabet, custom_alphabet)
        custom_b64 = standard_b64.translate(trans_table)

        return '{SRBX1}' + custom_b64

    def get_challenge(self, username: str, ip: str = "") -> Optional[str]:
        """
        获取认证Token
        :param username: 用户名
        :param ip: IP地址（可选）
        :return: Token字符串
        """
        url = f"{self.base_url}/cgi-bin/get_challenge"
        params = {
            'username': username,
            'ip': ip,
            'callback': f'jQuery_{int(time.time() * 1000)}'
        }

        try:
            resp = self.session.get(url, params=params, timeout=10)
            data = self._parse_jsonp(resp.text)

            if data.get('error') == 'ok':
                return data.get('challenge')
            else:
                print(f"获取Token失败: {data}")
                return None
        except Exception as e:
            print(f"获取Token异常: {e}")
            return None

    def get_user_info(self, ip: str = "") -> Optional[Dict[str, Any]]:
        """
        查询用户在线信息
        :param ip: IP地址（可选）
        :return: 用户信息字典或None
        """
        url = f"{self.base_url}/cgi-bin/rad_user_info"
        params = {
            'ip': ip,
            'callback': f'jQuery_{int(time.time() * 1000)}'
        }

        try:
            resp = self.session.get(url, params=params, timeout=10)
            data = self._parse_jsonp(resp.text)

            if data.get('error') == 'ok':
                return data
            else:
                return None
        except Exception as e:
            print(f"查询在线信息异常: {e}")
            return None

    def _detect_client_ip(self) -> str:
        """自动检测客户端IP地址"""
        try:
            # 尝试通过rad_user_info获取IP
            url = f"{self.base_url}/cgi-bin/rad_user_info"
            params = {'callback': f'jQuery_{int(time.time() * 1000)}'}
            resp = self.session.get(url, params=params, timeout=10)
            data = self._parse_jsonp(resp.text)

            # 从响应中获取client_ip
            if 'client_ip' in data:
                return data['client_ip']
            if 'online_ip' in data:
                return data['online_ip']
        except Exception as e:
            print(f"自动检测IP失败: {e}")

        return ""

    def login(self, username: str, password: str, ac_id: str = "1",
              ip: str = "", domain: str = "") -> bool:
        """
        用户认证登录
        :param username: 用户名
        :param password: 密码
        :param ac_id: AC ID（默认1）
        :param ip: IP地址（可选，留空自动检测）
        :param domain: 域（可选，如@cmcc）
        :return: 是否成功
        """
        # 自动检测IP
        if not ip:
            ip = self._detect_client_ip()
            if ip:
                print(f"检测到客户端IP: {ip}")

        # 拼接完整用户名
        full_username = username + domain

        # 获取Token
        token = self.get_challenge(full_username, ip)
        if not token:
            print("无法获取Token，登录失败")
            return False

        print(f"获取Token成功: {token}")

        # 计算MD5密码
        hmd5 = self._get_md5(password, token)

        # 构造用户信息
        user_info = {
            'username': full_username,
            'password': password,
            'ip': ip,
            'acid': ac_id,
            'enc_ver': 'srun_bx1'
        }

        print(f"用户信息: {user_info}")

        # 加密用户信息
        info = self._encode_user_info(user_info, token)
        print(f"加密后info: {info[:80]}...")

        # 计算checksum
        n = '200'
        type_val = '1'
        chksum_str = (
            f"{token}{full_username}{token}{hmd5}{token}{ac_id}"
            f"{token}{ip}{token}{n}{token}{type_val}{token}{info}"
        )
        chksum = self._get_sha1(chksum_str)

        # 构造登录请求参数
        params = {
            'action': 'login',
            'username': full_username,
            'password': f'{{MD5}}{hmd5}',
            'os': 'Windows 10',
            'name': 'Windows',
            'double_stack': '0',
            'chksum': chksum,
            'info': info,
            'ac_id': ac_id,
            'ip': ip,
            'n': n,
            'type': type_val,
            'callback': f'jQuery_{int(time.time() * 1000)}'
        }

        # 发送登录请求
        url = f"{self.base_url}/cgi-bin/srun_portal"

        try:
            resp = self.session.get(url, params=params, timeout=15)
            data = self._parse_jsonp(resp.text)

            print(f"登录响应: {data}")

            if data.get('error') == 'ok':
                print(f"[OK] 登录成功！")
                if 'suc_msg' in data:
                    print(f"  消息: {data['suc_msg']}")
                return True
            else:
                error_msg = data.get('error_msg', data.get('error', '未知错误'))
                print(f"[FAIL] 登录失败: {error_msg}")
                return False

        except Exception as e:
            print(f"登录请求异常: {e}")
            return False

    def logout(self, username: str, ac_id: str = "1",
               ip: str = "", domain: str = "") -> bool:
        """
        用户注销
        :param username: 用户名
        :param ac_id: AC ID
        :param ip: IP地址（可选）
        :param domain: 域（可选）
        :return: 是否成功
        """
        full_username = username + domain

        params = {
            'action': 'logout',
            'username': full_username,
            'ip': ip,
            'ac_id': ac_id,
            'callback': f'jQuery_{int(time.time() * 1000)}'
        }

        url = f"{self.base_url}/cgi-bin/srun_portal"

        try:
            resp = self.session.get(url, params=params, timeout=10)
            data = self._parse_jsonp(resp.text)

            print(f"注销响应: {data}")

            if data.get('error') == 'ok':
                print(f"[OK] 注销成功！")
                return True
            else:
                error_msg = data.get('error_msg', data.get('error', '未知错误'))
                print(f"[FAIL] 注销失败: {error_msg}")
                return False

        except Exception as e:
            print(f"注销请求异常: {e}")
            return False

    def keep_alive(self, username: str, password: str, ac_id: str = "1",
                   ip: str = "", domain: str = "", interval: int = 300):
        """
        保持在线（轮询检测并自动重连）
        :param username: 用户名
        :param password: 密码
        :param ac_id: AC ID
        :param ip: IP地址
        :param domain: 域
        :param interval: 检测间隔（秒，默认300秒）
        """
        print(f"开始保持在线，检测间隔: {interval}秒")
        print("按 Ctrl+C 停止...")

        try:
            while True:
                # 查询在线状态
                info = self.get_user_info(ip)

                if info:
                    print(f"[在线] 用户: {info.get('user_name', 'N/A')}, "
                          f"IP: {info.get('online_ip', 'N/A')}, "
                          f"已用流量: {info.get('sum_bytes', 0) / (1024**3):.2f} GB")
                else:
                    print("[离线] 检测到离线，尝试重新登录...")
                    self.login(username, password, ac_id, ip, domain)

                # 等待下一次检测
                time.sleep(interval)

        except KeyboardInterrupt:
            print("\n已停止保持在线")


def main():
    """主函数 - 示例用法"""
    # 配置参数
    BASE_URL = "http://10.0.100.100"
    USERNAME = "23882342"  # 替换为你的用户名
    PASSWORD = "www.2003"  # 替换为你的密码
    AC_ID = "1"
    DOMAIN = ""  # 如果有域，填写如"@cmcc"

    # 创建客户端
    client = SrunPortal(BASE_URL)

    # 示例1: 登录
    print("=" * 50)
    print("执行登录...")
    print("=" * 50)
    success = client.login(USERNAME, PASSWORD, AC_ID, domain=DOMAIN)

    if success:
        # 示例2: 查询在线信息
        print("\n" + "=" * 50)
        print("查询在线信息...")
        print("=" * 50)
        info = client.get_user_info()
        if info:
            print(f"用户名: {info.get('user_name')}")
            print(f"IP地址: {info.get('online_ip')}")
            print(f"已用流量: {info.get('sum_bytes', 0) / (1024**3):.2f} GB")
            print(f"已用时长: {info.get('sum_seconds', 0) // 3600} 小时")

        # 示例3: 保持在线（可选，取消注释启用）
        # print("\n" + "=" * 50)
        # print("保持在线...")
        # print("=" * 50)
        # client.keep_alive(USERNAME, PASSWORD, AC_ID, domain=DOMAIN, interval=300)

        # 示例4: 注销（可选）
        # print("\n" + "=" * 50)
        # print("执行注销...")
        # print("=" * 50)
        # client.logout(USERNAME, AC_ID, domain=DOMAIN)


if __name__ == "__main__":
    main()
