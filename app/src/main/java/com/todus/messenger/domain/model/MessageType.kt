package com.todus.messenger.domain.model

/**
 * Enum que representa los distintos tipos de mensaje soportados por ToDus.
 *
 * Cada tipo define cómo se debe renderizar y procesar el mensaje
 * en la interfaz de usuario y en la capa de datos.
 */
enum class MessageType {
    /** Mensaje de texto plano */
    TEXT {
        override fun isMediaType(): Boolean = false
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con imagen (JPEG, PNG, WebP, GIF) */
    IMAGE {
        override fun isMediaType(): Boolean = true
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con video (MP4, 3GP) */
    VIDEO {
        override fun isMediaType(): Boolean = true
        override fun hasDuration(): Boolean = true
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con audio o nota de voz (AAC, OGG, AMR) */
    AUDIO {
        override fun isMediaType(): Boolean = true
        override fun hasDuration(): Boolean = true
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con sticker/pegatina (WebP animado) */
    STICKER {
        override fun isMediaType(): Boolean = true
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con ubicación compartida (coordenadas GPS) */
    LOCATION {
        override fun isMediaType(): Boolean = false
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = true
    },

    /** Mensaje con contacto compartido (vCard) */
    CONTACT {
        override fun isMediaType(): Boolean = false
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = false
    },

    /** Mensaje con evento de calendario */
    EVENT {
        override fun isMediaType(): Boolean = false
        override fun hasDuration(): Boolean = false
        override fun hasLocation(): Boolean = false
    };

    /**
     * Indica si este tipo de mensaje contiene un archivo multimedia
     * que requiere descarga y visualización especial (imagen, video, audio, sticker).
     *
     * Los tipos TEXT, LOCATION, CONTACT y EVENT no son considerados multimedia.
     *
     * @return true si el tipo requiere manejo de archivos multimedia.
     */
    abstract fun isMediaType(): Boolean

    /**
     * Indica si este tipo de mensaje puede tener una duración asociada
     * (en segundos), útil para mostrar el tiempo de reproducción.
     *
     * Solo AUDIO y VIDEO tienen duración.
     *
     * @return true si el tipo admite duración.
     */
    abstract fun hasDuration(): Boolean

    /**
     * Indica si este tipo de mensaje contiene datos de ubicación GPS.
     *
     * Solo LOCATION tiene coordenadas.
     *
     * @return true si el tipo contiene ubicación.
     */
    abstract fun hasLocation(): Boolean

    /**
     * Indica si este tipo de mensaje soporta la generación de miniatura.
     *
     * @return true si el tipo puede tener una miniatura de vista previa.
     */
    fun supportsThumbnail(): Boolean = this == IMAGE || this == VIDEO

    /**
     * Indica si este tipo de mensaje es reproducible (audio o video).
     *
     * @return true si el tipo puede reproducirse.
     */
    fun isPlayable(): Boolean = this == AUDIO || this == VIDEO

    /**
     * Indica si este tipo de mensaje puede reenviarse.
     * Todos los tipos de mensaje pueden reenviarse.
     *
     * @return true siempre.
     */
    fun canBeForwarded(): Boolean = true

    /**
     * Indica si este tipo de mensaje puede responderse.
     * Todos los tipos de mensaje pueden responderse.
     *
     * @return true siempre.
     */
    fun canBeRepliedTo(): Boolean = true

    /**
     * Indica si este tipo de mensaje puede editarse.
     * Solo los mensajes de texto pueden editarse.
     *
     * @return true solo para TEXT.
     */
    fun canBeEdited(): Boolean = this == TEXT

    /**
     * Indica si este tipo de mensaje puede eliminarse.
     * Todos los tipos de mensaje pueden eliminarse.
     *
     * @return true siempre.
     */
    fun canBeDeleted(): Boolean = true

    companion object {
        /**
         * Obtiene el [MessageType] a partir de su nombre en cadena.
         * La comparación es insensible a mayúsculas y minúsculas.
         *
         * @param value Nombre del tipo de mensaje (ej: "IMAGE", "video").
         * @return El [MessageType] correspondiente, o TEXT por defecto si no coincide.
         */
        fun fromName(value: String): MessageType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                TEXT
            }
        }
    }
}
