import React, { useEffect, useMemo, useRef, useState } from 'react'
import deepEqual from 'fast-deep-equal'
import { Subscription } from 'rxjs'
import { useEditorContext } from '../context/EditorContextProvider'
import withWidgetDynamicProps from './withWidgetDynamicProps'
import { WidgetRenderer } from './WidgetRenderer'
import WidgetFormModel from '../form/WidgetFormModel'
import { WidgetProps, WidgetTreeNode } from '../types'
import { PanelAddEvent } from '../events'
import { BusEventType } from '../events/types'
import { PanelDeleteEvent, PanelEditEvent, PanelPreviewEvent, PanelSaveEvent, SectionAddEvent } from '../events/app'
import WidgetFormModal from '../form/WidgetFormModal'
import WidgetPreviewModal from '../form/WidgetPreviewModal'
import useRestoreState from './hooks/useRestoreState'

export const WrapedWidgetNode = withWidgetDynamicProps(function WidgetNode(props: WidgetProps) {
    const { childWidgets, path = [] } = props
    return (
        <WidgetRenderer {...props}>
            {childWidgets &&
                childWidgets.length > 0 &&
                childWidgets.map((node, i) => {
                    const { children: childChildren, id, ...childRest } = node ?? {}
                    return (
                        <WrapedWidgetNode
                            key={id ?? i}
                            id={id}
                            path={[...path, 'children', i]}
                            childWidgets={childChildren}
                            {...childRest}
                        />
                    )
                })}
        </WidgetRenderer>
    )
})

export function WidgetRenderTree({ initialState, onStateChange }: any) {
    const { store, eventBus, dynamicVars } = useEditorContext()
    const { storeKey: key, projectId } = dynamicVars
    const api = store()
    const tree = store((state) => state.tree, deepEqual)
    // @ts-ignore
    const [editWidget, setEditWidget] = useState<BusEventType>(null)
    const [isPanelModalOpen, setisPanelModalOpen] = React.useState(false)
    const [viewWidget, setViewWidget] = useState<PanelPreviewEvent>()
    const [isPanelPreviewModalOpen, setisPanelPreviewModalOpen] = React.useState(false)
    const form = useRef(new WidgetFormModel().initPanelSchema())

    const { toSave } = useRestoreState(store, initialState, dynamicVars)

    // @ts-ignore
    const handleAddSection = ({ path, type }) => {
        api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
            type,
        })
    }

    const handleAddPanel = (formData: any) => {
        const { path } = editWidget?.payload
        if (path && path.length > 0)
            api.onLayoutChildrenChange(['tree', ...path], ['tree', ...path, 'children'], {
                type: formData.chartType,
                fieldConfig: {
                    data: formData,
                },
            })
    }

    const handleEditPanel = (formData: any) => {
        const { id } = editWidget?.payload
        api.onWidgetChange(id, {
            type: formData.chartType,
            fieldConfig: {
                data: formData,
            },
        })
    }

    const handelDeletePanel = (evt: PanelDeleteEvent) => {
        const { id } = evt?.payload
        api.onWidgetDelete(id)
    }

    const actions = {
        [PanelAddEvent.type]: handleAddPanel,
        [PanelEditEvent.type]: handleEditPanel,
    }

    // subscription
    useEffect(() => {
        const subscription = new Subscription()
        subscription.add(
            eventBus.getStream(PanelAddEvent).subscribe({
                next: (evt) => {
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelEditEvent).subscribe({
                next: (evt) => {
                    setisPanelModalOpen(true)
                    setEditWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelDeleteEvent).subscribe({
                next: (evt) => handelDeletePanel(evt),
            })
        )
        subscription.add(
            eventBus.getStream(PanelPreviewEvent).subscribe({
                next: (evt) => {
                    setisPanelPreviewModalOpen(true)
                    setViewWidget(evt)
                },
            })
        )
        subscription.add(
            eventBus.getStream(SectionAddEvent).subscribe({
                next: (evt) => {
                    handleAddSection(evt.payload)
                },
            })
        )
        subscription.add(
            eventBus.getStream(PanelSaveEvent).subscribe({
                next: async () => {
                    onStateChange?.(toSave())
                },
            })
        )
        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId, store, key, eventBus])

    const Nodes = useMemo(() => {
        return tree.map((node: WidgetTreeNode, i: number) => (
            <WrapedWidgetNode key={node.id} id={node.id} type={node.type} path={[i]} childWidgets={node.children} />
        ))
    }, [tree])

    return (
        <div>
            {Nodes}
            <WidgetFormModal
                form={form.current}
                id={editWidget?.payload?.id}
                isShow={isPanelModalOpen}
                setIsShow={setisPanelModalOpen}
                store={store}
                handleFormSubmit={({ formData }: any) => {
                    // @ts-ignore
                    actions[editWidget?.type]?.(formData)
                    setisPanelModalOpen(false)
                }}
            />
            <WidgetPreviewModal
                id={viewWidget?.payload?.id}
                isShow={isPanelPreviewModalOpen}
                setIsShow={setisPanelPreviewModalOpen}
                store={store}
            />
        </div>
    )
}

export default WidgetRenderTree
