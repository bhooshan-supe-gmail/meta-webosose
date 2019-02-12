# Copyright (c) 2015-2019 LG Electronics, Inc.

SUMMARY = "WebAppMgr is responsible for running web applications on webOS"
AUTHOR = "Lokesh Kumar Goel <lokeshkumar.goel@lge.com>"
SECTION = "webos/base"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "virtual/webruntime qtbase luna-service2 sqlite3 librolegen nyx-lib openssl luna-prefs libpbnjson freetype serviceinstaller glib-2.0 pmloglib lttng-ust"
PROVIDES = "webappmanager-webos"

# webappmgr's upstart conf expects to be able to LD_PRELOAD ptmalloc3
RDEPENDS_${PN} = "ptmalloc3"
# webappmgr's upstart conf expects to have ionice available. Under OE-core, this is supplied by util-linux.
RDEPENDS_${PN} += "util-linux"
RDEPENDS_${PN} += "qtbase-plugins"

#  webappmgr2's upstart conf expects setcpushares-task to be available
VIRTUAL-RUNTIME_cpushareholder ?= "cpushareholder-stub"
RDEPENDS_${PN} += "${VIRTUAL-RUNTIME_cpushareholder}"

WEBOS_VERSION = "1.0.1-5_415bf77bdcba289a967af16a1bffd7d424fb17b7"
PR = "r24"

inherit webos_enhanced_submissions
inherit webos_system_bus
inherit webos_machine_dep
inherit webos_qmake5
inherit webos_lttng
inherit webos_distro_variant_dep
inherit webos_distro_dep
inherit webos_public_repo

WAM_DATA_DIR = "${webos_execstatedir}/${BPN}"

SRC_URI = "${WEBOSOSE_GIT_REPO_COMPLETE}"
S = "${WORKDIR}/git"

WEBOS_SYSTEM_BUS_SKIP_DO_TASKS = "1"

WEBOS_SYSTEM_BUS_FILES_LOCATION = ""
SYSTEMD_INSTALL_PATH = "${sysconfdir}/systemd/system"

OE_QMAKE_PATH_HEADERS = "${OE_QMAKE_PATH_QT_HEADERS}"

WEBOS_QMAKE_TARGET = "${MACHINE}"

# Set the location of chromium headers
EXTRA_QMAKEVARS_PRE += "CHROMIUM_SRC_DIR=${STAGING_INCDIR}/${PREFERRED_PROVIDER_virtual/webruntime}"

# Enable LTTng tracing capability when enabled in webos_lttng class
EXTRA_QMAKEVARS_PRE += "${@oe.utils.conditional('WEBOS_LTTNG_ENABLED', '1', 'CONFIG+=lttng', '', d)}"

EXTRA_QMAKEVARS_PRE += "DEFINES+=WAM_DATA_DIR=\"\"${webos_cryptofsdir}/.webappmanager/\"\""
EXTRA_QMAKEVARS_PRE += "PREFIX=/usr"
EXTRA_QMAKEVARS_PRE += "PLATFORM=${@'PLATFORM_' + '${DISTRO}'.upper().replace('-', '_')}"

# chromium doesn't build for armv[45]*
COMPATIBLE_MACHINE = "(-)"
COMPATIBLE_MACHINE_aarch64 = "(.*)"
COMPATIBLE_MACHINE_armv6 = "(.*)"
COMPATIBLE_MACHINE_armv7a = "(.*)"
COMPATIBLE_MACHINE_armv7ve = "(.*)"
COMPATIBLE_MACHINE_x86 = "(.*)"
COMPATIBLE_MACHINE_x86-64 = "(.*)"

WAM_ERROR_SCRIPTS_PATH = "${S}/html-ose"

do_configure_append_qemux86() {
    # Remove this condition once webos wam is synchronized to get systemd initscripts
    if [ -f "${S}/files/launch/systemd/webapp-mgr.sh.in" ]; then
        # Disable media hardware acceleration
        sed -i '/--enable-aggressive-release-policy \\/a\   --disable-web-media-player-neva \\' ${S}/files/launch/systemd/webapp-mgr.sh.in
    fi
}

do_install_append() {
    install -d ${D}${sysconfdir}/pmlog.d
    install -d ${D}${sysconfdir}/wam
    install -d ${D}${WAM_DATA_DIR}
    install -v -m 644 ${S}/files/launch/security_policy.conf ${D}${sysconfdir}/wam/security_policy.conf
    # add loaderror.html and geterror.js to next to resources directory (webos_localization_resources_dir)
    install -d ${D}${datadir}/localization/${BPN}/
    install -d ${D}${SYSTEMD_INSTALL_PATH}/scripts/
    install -v -m 644 ${S}/files/launch/systemd/webapp-mgr.service ${D}${SYSTEMD_INSTALL_PATH}/webapp-mgr.service
    install -v -m 755 ${S}/files/launch/systemd/webapp-mgr.sh.in ${D}${SYSTEMD_INSTALL_PATH}/scripts/webapp-mgr.sh
    cp -vf ${WAM_ERROR_SCRIPTS_PATH}/* ${D}${datadir}/localization/${BPN}/
    # TODO: Drop this code when chromium68 is ACG complaint
    if [ "${PREFERRED_PROVIDER_virtual/webruntime}" = "webruntime" ]; then
        install -d ${D}${webos_sysbus_pubservicesdir}
        install -d ${D}${webos_sysbus_pubrolesdir}
        install -d ${D}${webos_sysbus_prvservicesdir}
        install -d ${D}${webos_sysbus_prvrolesdir}
        install -v -m 0644 ${S}/files/sysbus/com.palm.webappmgr.service.pub ${D}${webos_sysbus_pubservicesdir}/com.palm.webappmgr.service
        install -v -m 0644 ${S}/files/sysbus/com.palm.webappmgr.service.prv ${D}${webos_sysbus_prvservicesdir}/com.palm.webappmgr.service
        install -v -m 0644 ${S}/files/sysbus/com.palm.webappmgr.json.prv ${D}${webos_sysbus_prvrolesdir}/com.palm.webappmgr.json
        install -v -m 0644 ${S}/files/sysbus/com.palm.webappmgr.json.pub ${D}${webos_sysbus_pubrolesdir}/com.palm.webappmgr.json
    fi
}

FILES_${PN} += " \
    ${sysconfdir}/pmlog.d \
    ${SYSTEMD_INSTALL_PATH} \
    ${sysconfdir}/wam \
    ${libdir}/webappmanager/plugins/*.so \
    ${datadir}/localization/${BPN} \
    ${WEBOS_SYSTEM_BUS_DIRS} \
"
