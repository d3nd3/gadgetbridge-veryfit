package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.prg

@Suppress("ClassName")
sealed class PrgSectionMagic(open val magic: UInt) {
    object END : PrgSectionMagic(0x00000000.toUInt())
    object HEAD : PrgSectionMagic(0xd000d000.toUInt())
    object HEAD_VERSIONED : PrgSectionMagic(0xd000d00d.toUInt())
    object CODE : PrgSectionMagic(0xc0debabe.toUInt())
    object CODE_EXTENDED : PrgSectionMagic(0xc0de10ad.toUInt())
    object DATA : PrgSectionMagic(0xda7ababe.toUInt())
    object PC_TO_LINE_NUM : PrgSectionMagic(0xc0de7ab1.toUInt())
    object ENTRY_POINTS : PrgSectionMagic(0x6060c0de.toUInt())
    object LINK_TABLE : PrgSectionMagic(0xc1a557b1.toUInt())
    object PERMISSIONS : PrgSectionMagic(0x6000db01.toUInt())
    object EXCEPTIONS : PrgSectionMagic(0x0ece7105.toUInt())
    object SYMBOLS : PrgSectionMagic(0x5717b015.toUInt())
    object STRING_RESOURCE_SYMBOLS : PrgSectionMagic(0xbaada555.toUInt())
    object SETTINGS : PrgSectionMagic(0x5e771465.toUInt())
    object APP_UNLOCK : PrgSectionMagic(0xd011aaa5.toUInt())
    object RESOURCE : PrgSectionMagic(0xf00d600d.toUInt())
    object BACKGROUND_RESOURCE : PrgSectionMagic(0xdefeca7e.toUInt())
    object GLANCE_RESOURCE : PrgSectionMagic(0xd00dface.toUInt())
    object APP_STORE_SIGNATURE : PrgSectionMagic(0x00005161.toUInt())
    object DEVELOPER_SIGNATURE : PrgSectionMagic(0xe1c0de12.toUInt())
    object DEBUG : PrgSectionMagic(0xd0000d1e.toUInt())
    object COMPLICATION : PrgSectionMagic(0xfaceda7a.toUInt())
    data class UNKNOWN(override val magic: UInt) : PrgSectionMagic(magic)

    companion object {
        fun fromMagic(magic: UInt): PrgSectionMagic {
            return when (magic) {
                END.magic -> END
                HEAD.magic -> HEAD
                HEAD_VERSIONED.magic -> HEAD_VERSIONED
                CODE.magic -> CODE
                CODE_EXTENDED.magic -> CODE_EXTENDED
                DATA.magic -> DATA
                PC_TO_LINE_NUM.magic -> PC_TO_LINE_NUM
                ENTRY_POINTS.magic -> ENTRY_POINTS
                LINK_TABLE.magic -> LINK_TABLE
                PERMISSIONS.magic -> PERMISSIONS
                EXCEPTIONS.magic -> EXCEPTIONS
                SYMBOLS.magic -> SYMBOLS
                STRING_RESOURCE_SYMBOLS.magic -> STRING_RESOURCE_SYMBOLS
                SETTINGS.magic -> SETTINGS
                APP_UNLOCK.magic -> APP_UNLOCK
                RESOURCE.magic -> RESOURCE
                BACKGROUND_RESOURCE.magic -> BACKGROUND_RESOURCE
                GLANCE_RESOURCE.magic -> GLANCE_RESOURCE
                APP_STORE_SIGNATURE.magic -> APP_STORE_SIGNATURE
                DEVELOPER_SIGNATURE.magic -> DEVELOPER_SIGNATURE
                DEBUG.magic -> DEBUG
                COMPLICATION.magic -> COMPLICATION
                else -> UNKNOWN(magic)
            }
        }
    }
}
