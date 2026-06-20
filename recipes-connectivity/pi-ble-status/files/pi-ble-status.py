#!/usr/bin/env python3
"""BLE GATT server exposing Pi status as readable characteristics."""

import sys
import socket
import subprocess
import dbus
import dbus.service
import dbus.mainloop.glib
from gi.repository import GLib

BLUEZ_SVC       = 'org.bluez'
GATT_MGR_IFACE  = 'org.bluez.GattManager1'
GATT_SVC_IFACE  = 'org.bluez.GattService1'
GATT_CHR_IFACE  = 'org.bluez.GattCharacteristic1'
LE_ADV_MGR      = 'org.bluez.LEAdvertisingManager1'
LE_ADV_IFACE    = 'org.bluez.LEAdvertisement1'
OM_IFACE        = 'org.freedesktop.DBus.ObjectManager'
PROP_IFACE      = 'org.freedesktop.DBus.Properties'

SVC_UUID      = '00001000-0000-0000-0000-000000000000'
WIFI_IP_UUID  = '00001001-0000-0000-0000-000000000000'
ETH_IP_UUID   = '00001002-0000-0000-0000-000000000000'
TEMP_UUID     = '00001003-0000-0000-0000-000000000000'
UPTIME_UUID   = '00001004-0000-0000-0000-000000000000'
HOST_UUID     = '00001005-0000-0000-0000-000000000000'


# --- Status helpers ----------------------------------------------------------

def get_ip(iface):
    try:
        out = subprocess.check_output(
            ['ip', '-4', 'addr', 'show', iface],
            stderr=subprocess.DEVNULL, text=True)
        for line in out.splitlines():
            if 'inet ' in line:
                return line.strip().split()[1].split('/')[0]
    except Exception:
        pass
    return 'N/A'

def get_temp():
    try:
        with open('/sys/class/thermal/thermal_zone0/temp') as f:
            return f'{int(f.read().strip()) / 1000:.1f}C'
    except Exception:
        return 'N/A'

def get_uptime():
    try:
        with open('/proc/uptime') as f:
            secs = int(float(f.read().split()[0]))
            h, rem = divmod(secs, 3600)
            m, s = divmod(rem, 60)
            return f'{h}h {m}m {s}s'
    except Exception:
        return 'N/A'

def to_bytes(s):
    return dbus.Array([dbus.Byte(b) for b in s.encode()], signature='y')


# --- GATT objects ------------------------------------------------------------

class Application(dbus.service.Object):
    def __init__(self, bus):
        self._services = []
        dbus.service.Object.__init__(self, bus, '/org/bluez/pistatus')

    def add_service(self, svc):
        self._services.append(svc)

    @dbus.service.method(OM_IFACE, out_signature='a{oa{sa{sv}}}')
    def GetManagedObjects(self):
        objs = {}
        for svc in self._services:
            objs[svc.path] = svc.props()
            for c in svc.chars:
                objs[c.path] = c.props()
        return objs


class Service(dbus.service.Object):
    def __init__(self, bus, index, uuid):
        self.path = dbus.ObjectPath(f'/org/bluez/pistatus/svc{index}')
        self.uuid = uuid
        self.chars = []
        dbus.service.Object.__init__(self, bus, self.path)

    def add_char(self, c):
        self.chars.append(c)

    def props(self):
        return {GATT_SVC_IFACE: {
            'UUID': self.uuid,
            'Primary': dbus.Boolean(True),
            'Characteristics': dbus.Array([c.path for c in self.chars], signature='o'),
        }}

    @dbus.service.method(PROP_IFACE, in_signature='s', out_signature='a{sv}')
    def GetAll(self, iface):
        return self.props()[GATT_SVC_IFACE]


class Characteristic(dbus.service.Object):
    def __init__(self, bus, index, uuid, svc, value_fn):
        self.path = dbus.ObjectPath(f'{svc.path}/char{index}')
        self.uuid = uuid
        self.svc = svc
        self.value_fn = value_fn
        dbus.service.Object.__init__(self, bus, self.path)

    def props(self):
        return {GATT_CHR_IFACE: {
            'UUID': self.uuid,
            'Service': self.svc.path,
            'Flags': dbus.Array(['read'], signature='s'),
        }}

    @dbus.service.method(PROP_IFACE, in_signature='s', out_signature='a{sv}')
    def GetAll(self, iface):
        return self.props()[GATT_CHR_IFACE]

    @dbus.service.method(GATT_CHR_IFACE, in_signature='a{sv}', out_signature='ay')
    def ReadValue(self, options):
        return to_bytes(self.value_fn())


class Advertisement(dbus.service.Object):
    def __init__(self, bus):
        dbus.service.Object.__init__(self, bus, '/org/bluez/pistatusadv')

    @dbus.service.method(PROP_IFACE, in_signature='s', out_signature='a{sv}')
    def GetAll(self, iface):
        return {
            'Type': dbus.String('peripheral'),
            'LocalName': dbus.String(socket.gethostname()),
            'ServiceUUIDs': dbus.Array([SVC_UUID], signature='s'),
            'Includes': dbus.Array(['tx-power'], signature='s'),
        }

    @dbus.service.method(LE_ADV_IFACE)
    def Release(self):
        pass


# --- Main --------------------------------------------------------------------

def find_adapter(bus):
    om = dbus.Interface(bus.get_object(BLUEZ_SVC, '/'), OM_IFACE)
    for path, ifaces in om.GetManagedObjects().items():
        if GATT_MGR_IFACE in ifaces:
            return path
    return None


def main():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SystemBus()

    adapter = find_adapter(bus)
    if not adapter:
        print('No Bluetooth adapter found', file=sys.stderr)
        sys.exit(1)

    # Power on adapter
    props = dbus.Interface(bus.get_object(BLUEZ_SVC, adapter), PROP_IFACE)
    props.Set('org.bluez.Adapter1', 'Powered', dbus.Boolean(True))

    # Build GATT application
    app = Application(bus)
    svc = Service(bus, 0, SVC_UUID)
    for i, (uuid, fn) in enumerate([
        (WIFI_IP_UUID, lambda: get_ip('wlan0')),
        (ETH_IP_UUID,  lambda: get_ip('eth0')),
        (TEMP_UUID,    get_temp),
        (UPTIME_UUID,  get_uptime),
        (HOST_UUID,    socket.gethostname),
    ]):
        svc.add_char(Characteristic(bus, i, uuid, svc, fn))
    app.add_service(svc)

    # Register GATT application
    gatt = dbus.Interface(bus.get_object(BLUEZ_SVC, adapter), GATT_MGR_IFACE)
    gatt.RegisterApplication(
        dbus.ObjectPath('/org/bluez/pistatus'), {},
        reply_handler=lambda: print('GATT registered'),
        error_handler=lambda e: (print(f'GATT error: {e}', file=sys.stderr), sys.exit(1)))

    # Register advertisement
    adv = Advertisement(bus)
    adv_mgr = dbus.Interface(bus.get_object(BLUEZ_SVC, adapter), LE_ADV_MGR)
    adv_mgr.RegisterAdvertisement(
        dbus.ObjectPath('/org/bluez/pistatusadv'), {},
        reply_handler=lambda: print('Advertising as', socket.gethostname()),
        error_handler=lambda e: print(f'Adv error: {e}', file=sys.stderr))

    GLib.MainLoop().run()


if __name__ == '__main__':
    main()
