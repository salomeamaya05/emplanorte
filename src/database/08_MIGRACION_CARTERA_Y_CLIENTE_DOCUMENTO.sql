-- Migración aditiva para compatibilizar Supabase con las entidades JPA actuales.
-- No elimina ni reescribe ventas, clientes o movimientos existentes.

ALTER TABLE public.clientes
    ADD COLUMN IF NOT EXISTS documento varchar(40);

CREATE UNIQUE INDEX IF NOT EXISTS uq_clientes_documento
    ON public.clientes (documento)
    WHERE documento IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.creditos_venta (
    id                  bigserial       PRIMARY KEY,
    id_venta            bigint          NOT NULL,
    id_cliente          bigint          NOT NULL,
    total_credito       numeric(14,2)   NOT NULL,
    saldo_pendiente     numeric(14,2)   NOT NULL,
    fecha_vencimiento   date            NOT NULL,
    estado              varchar(20)     NOT NULL DEFAULT 'pendiente',
    observaciones       text,
    version             bigint          NOT NULL DEFAULT 0,
    creado_en           timestamp       NOT NULL DEFAULT now(),
    actualizado_en      timestamp       NOT NULL DEFAULT now(),
    CONSTRAINT uq_creditos_venta UNIQUE (id_venta),
    CONSTRAINT fk_creditos_venta_venta
        FOREIGN KEY (id_venta) REFERENCES public.ventas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_creditos_venta_cliente
        FOREIGN KEY (id_cliente) REFERENCES public.clientes(id) ON DELETE RESTRICT,
    CONSTRAINT chk_creditos_total_positivo CHECK (total_credito > 0),
    CONSTRAINT chk_creditos_saldo_valido
        CHECK (saldo_pendiente >= 0 AND saldo_pendiente <= total_credito),
    CONSTRAINT chk_creditos_estado
        CHECK (estado IN ('pendiente', 'pagado', 'anulado'))
);

CREATE INDEX IF NOT EXISTS idx_creditos_estado_vencimiento
    ON public.creditos_venta (estado, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_creditos_cliente
    ON public.creditos_venta (id_cliente);

CREATE TABLE IF NOT EXISTS public.abonos_credito (
    id                      bigserial       PRIMARY KEY,
    id_credito              bigint          NOT NULL,
    id_usuario              bigint          NOT NULL,
    monto                   numeric(14,2)   NOT NULL,
    forma_pago              varchar(30)     NOT NULL,
    fecha_pago              timestamp       NOT NULL,
    tipo                    varchar(20)     NOT NULL DEFAULT 'abono',
    observaciones           text,
    clave_idempotencia      varchar(80)     NOT NULL,
    creado_en               timestamp       NOT NULL DEFAULT now(),
    CONSTRAINT uq_abonos_credito_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT fk_abonos_credito_credito
        FOREIGN KEY (id_credito) REFERENCES public.creditos_venta(id) ON DELETE RESTRICT,
    CONSTRAINT fk_abonos_credito_usuario
        FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT chk_abonos_credito_monto CHECK (monto > 0),
    CONSTRAINT chk_abonos_credito_tipo CHECK (tipo IN ('inicial', 'abono'))
);

CREATE INDEX IF NOT EXISTS idx_abonos_credito_fecha
    ON public.abonos_credito (id_credito, fecha_pago);
CREATE INDEX IF NOT EXISTS idx_abonos_credito_usuario
    ON public.abonos_credito (id_usuario);

-- Las tablas nuevas nacen cerradas para el Data API. El backend JDBC utiliza
-- postgres y conserva acceso porque no se aplica FORCE RLS.
ALTER TABLE public.creditos_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.abonos_credito ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON TABLE public.creditos_venta, public.abonos_credito
    FROM anon, authenticated;
REVOKE ALL PRIVILEGES ON SEQUENCE public.creditos_venta_id_seq,
    public.abonos_credito_id_seq FROM anon, authenticated;
