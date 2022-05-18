import React from 'react'
import { Link } from 'react-router-dom'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import smallLogoImg from '@/assets/logo_small_en_white.svg'
import normalLogoImg from '@/assets/logo_normal_en_white.svg'
import { IComposedComponentProps } from '@/theme'
import { createUseStyles } from 'react-jss'
import classNames from 'classnames'
import { sidebarFoldedWidth, sidebarExpandedWidth, headerHeight } from '@/consts'

const useLogoStyles = createUseStyles({
    logoWrapper: {
        display: 'flex',
        flexDirection: 'row',
        textDecoration: 'none',
        alignItems: 'center',
        justifyContent: 'center',
        transition: 'width 100ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
        height: headerHeight,
    },
})

export interface ILogoProps extends IComposedComponentProps {
    expanded?: boolean
}

export default function Logo({ expanded = true, className }: ILogoProps) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const styles = useLogoStyles()

    const LogoText = expanded ? (
        <img style={{ width: '140px' }} src={normalLogoImg} alt='logo' />
    ) : (
        <img style={{ width: '50px' }} src={smallLogoImg} alt='logo' />
    )

    if (!currentUser)
        return (
            <div
                className={classNames(styles.logoWrapper, className)}
                style={{
                    width: expanded ? sidebarExpandedWidth : sidebarFoldedWidth,
                }}
            >
                {LogoText}
            </div>
        )

    return (
        <Link
            className={classNames(styles.logoWrapper, className)}
            style={{
                width: expanded ? sidebarExpandedWidth : sidebarFoldedWidth,
            }}
            to='/'
        >
            {LogoText}
        </Link>
    )
}
